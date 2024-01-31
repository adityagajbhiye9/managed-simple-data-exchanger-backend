/********************************************************************************
 * Copyright (c) 2022, 2023 T-Systems International GmbH
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.edc.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.bpndiscovery.handler.BpnDiscoveryProxyService;
import org.eclipse.tractusx.sde.bpndiscovery.model.request.BpnDiscoverySearchRequest;
import org.eclipse.tractusx.sde.bpndiscovery.model.request.BpnDiscoverySearchRequest.Search;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoveryResponse;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoverySearchResponse;
import org.eclipse.tractusx.sde.common.exception.ServiceException;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.SubModelResponse;
import org.eclipse.tractusx.sde.edc.entities.database.ContractNegotiationInfoEntity;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ActionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.facilitator.ContractNegotiateManagementHelper;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.gateways.database.ContractNegotiationInfoRepository;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.ContractNegotiationDto;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedResponse;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.util.EDCAssetUrlCacheService;
import org.eclipse.tractusx.sde.edc.util.UtilityFunctions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerControlPanelService {

	private static final String NEGOTIATED = "NEGOTIATED";
	private static final String STATUS = "status";

	private final ContractNegotiateManagementHelper contractNegotiateManagement;

	private final ContractNegotiationInfoRepository contractNegotiationInfoRepository;
	private final PolicyConstraintBuilderService policyConstraintBuilderService;

	private final EDRRequestHelper edrRequestHelper;
	private final BpnDiscoveryProxyService bpnDiscoveryProxyService;
	private final EDCAssetUrlCacheService edcAssetUrlCacheService;
	private final CatalogResponseBuilder catalogResponseBuilder;
	private final ContractNegotiationService contractNegotiationService;
	private final LookUpDTTwin lookUpDTTwin;

	String filterExpressionTemplate = """
			"filterExpression": [
				    {
				        "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
				        "operator": "=",
				        "operandRight": "%s"
				    }
				]
			""";

	public List<QueryDataOfferModel> queryOnDataOffers(String manufacturerPartId, String searchBpnNumber,
			String submodel, Integer offset, Integer limit) {

		List<QueryDataOfferModel> result = new ArrayList<>();
		List<String> bpnList = null;

		// 1 find bpn if empty using BPN discovery
		if (StringUtils.isBlank(searchBpnNumber)) {
			BpnDiscoverySearchRequest bpnDiscoverySearchRequest = BpnDiscoverySearchRequest.builder()
					.searchFilter(List
							.of(Search.builder().type("manufacturerPartId").keys(List.of(manufacturerPartId)).build()))
					.build();

			BpnDiscoverySearchResponse bpnDiscoverySearchData = bpnDiscoveryProxyService
					.bpnDiscoverySearchData(bpnDiscoverySearchRequest);

			bpnList = bpnDiscoverySearchData.getBpns().stream().map(BpnDiscoveryResponse::getValue).toList();

		} else {
			bpnList = List.of(searchBpnNumber);
		}

		for (String bpnNumber : bpnList) {

			// 2 fetch EDC connectors and DTR Assets from EDC connectors
			List<QueryDataOfferModel> ddTROffers = edcAssetUrlCacheService.getDDTRUrl(bpnNumber);

			// 3 lookup shell for PCF sub model
			for (QueryDataOfferModel dtOffer : ddTROffers) {

				EDRCachedByIdResponse edrToken = edcAssetUrlCacheService.verifyAndGetToken(bpnNumber, dtOffer);
				if (edrToken != null) {

					SubModelResponse lookUpTwin = lookUpDTTwin.lookUpTwin(edrToken, dtOffer, manufacturerPartId,
							bpnNumber, submodel);

					if (lookUpTwin != null) {
						String subprotocolBody = lookUpTwin.getEndpoints().get(0).getProtocolInformation()
								.getSubprotocolBody();

						String[] edcInfo = subprotocolBody.split(";");
						String[] assetInfo = edcInfo[0].split("=");
						String[] connectorInfo = edcInfo[1].split("=");

						String filterExpression = String.format(filterExpressionTemplate, assetInfo[1]);

						List<QueryDataOfferModel> queryOnDataOffers = catalogResponseBuilder
								.queryOnDataOffers(connectorInfo[1], offset, limit, filterExpression);
						result.addAll(queryOnDataOffers);
					}
				} else {
					log.warn("EDR token is null, unable to look Up PCF Twin");
				}
			}
		}
		return result;

	}

	@Async
	public void subscribeDataOffers(ConsumerRequest consumerRequest, String processId) {

		HashMap<String, String> extensibleProperty = new HashMap<>();
		AtomicReference<String> negotiateContractId = new AtomicReference<>();
		AtomicReference<ContractNegotiationDto> checkContractNegotiationStatus = new AtomicReference<>();

		ActionRequest action = policyConstraintBuilderService
				.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies());

		consumerRequest.getOffers().parallelStream().forEach(offer -> {
			try {
				negotiateContractId.set(contractNegotiateManagement.negotiateContract(consumerRequest.getProviderUrl(),
						consumerRequest.getConnectorId(), offer.getOfferId(), offer.getAssetId(), action,
						extensibleProperty));
				int retry = 3;
				int counter = 1;

				do {
					Thread.sleep(3000);
					checkContractNegotiationStatus
							.set(contractNegotiateManagement.checkContractNegotiationStatus(negotiateContractId.get()));
					counter++;
				} while (checkContractNegotiationStatus.get() != null
						&& !checkContractNegotiationStatus.get().getState().equals("FINALIZED")
						&& !checkContractNegotiationStatus.get().getState().equals("TERMINATED") && counter <= retry);

			} catch (InterruptedException ie) {
				log.error("Exception in subscribeDataOffers" + ie.getMessage());
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Exception in subscribeDataOffers" + e.getMessage());
			} finally {
				ContractNegotiationInfoEntity contractNegotiationInfoEntity = ContractNegotiationInfoEntity.builder()
						.id(UUID.randomUUID().toString()).processId(processId)
						.connectorId(consumerRequest.getConnectorId()).offerId(offer.getOfferId())
						.contractNegotiationId(negotiateContractId != null ? negotiateContractId.get() : null)
						.status(checkContractNegotiationStatus.get() != null
								? checkContractNegotiationStatus.get().getState()
								: "Failed:Exception")
						.dateTime(LocalDateTime.now()).build();

				contractNegotiationInfoRepository.save(contractNegotiationInfoEntity);
			}
		});

	}

	public Map<String, Object> subscribeAndDownloadDataOffers(ConsumerRequest consumerRequest,
			boolean flagToDownloadImidiate) {
		HashMap<String, String> extensibleProperty = new HashMap<>();
		Map<String, Object> response = new ConcurrentHashMap<>();

		var recipientURL = UtilityFunctions.removeLastSlashOfUrl(consumerRequest.getProviderUrl());

		ActionRequest action = policyConstraintBuilderService
				.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies());
		consumerRequest.getOffers().parallelStream().forEach(offer -> {
			Map<String, Object> resultFields = new ConcurrentHashMap<>();
			try {
				EDRCachedResponse checkContractNegotiationStatus = contractNegotiationService
						.verifyOrCreateContractNegotiation(consumerRequest.getConnectorId(), extensibleProperty,
								recipientURL, action, offer);

				resultFields.put("edr", checkContractNegotiationStatus);

				doVerifyResult(offer.getAssetId(), checkContractNegotiationStatus);

				if (flagToDownloadImidiate)
					resultFields.put("data",
							downloadFile(checkContractNegotiationStatus, consumerRequest.getDownloadDataAs()));

				resultFields.put(STATUS, "SUCCESS");

			} catch (FeignException e) {
				log.error("Feign RequestBody: " + e.request());
				String errorMsg = "Unable to complete subscribeAndDownloadDataOffers because: " + e.contentUTF8();
				log.error(errorMsg);
				prepareErrorMap(resultFields, errorMsg);
			} catch (Exception e) {
				log.error("SubscribeAndDownloadDataOffers Oops! We have -" + e.getMessage());
				String errorMsg = "Unable to complete subscribeAndDownloadDataOffers because: " + e.getMessage();
				prepareErrorMap(resultFields, errorMsg);
			} finally {
				response.put(offer.getAssetId(), resultFields);
			}
		});
		return response;
	}

	@SneakyThrows
	private void doVerifyResult(String assetId, EDRCachedResponse checkContractNegotiationStatus)
			throws ServiceException {

		if (checkContractNegotiationStatus != null
				&& StringUtils.isBlank(checkContractNegotiationStatus.getTransferProcessId())
				&& StringUtils.isNoneBlank(checkContractNegotiationStatus.getAgreementId())) {
			throw new ServiceException("There is valid contract agreement exist for " + assetId
					+ " but intiate data transfer is not completed and no EDR token available, download is not possible");
		}

		String state = Optional.ofNullable(checkContractNegotiationStatus).filter(
				verifyEDRRequestStatusLocal -> NEGOTIATED.equalsIgnoreCase(verifyEDRRequestStatusLocal.getEdrState()))
				.map(EDRCachedResponse::getEdrState)
				.orElseThrow(() -> new ServiceException(
						"Time out!! to get 'NEGOTIATED' EDC EDR status to download data, the current status is '"
								+ checkContractNegotiationStatus.getEdrState() + "'"));
		log.info("The EDR token status :" + state);
	}

	@SneakyThrows
	public Map<String, Object> downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(List<String> assetIdList,
			String type) {
		Map<String, Object> response = new ConcurrentHashMap<>();
		assetIdList.parallelStream().forEach(assetId -> {

			Map<String, Object> downloadResultFields = new ConcurrentHashMap<>();
			try {
				EDRCachedResponse verifyEDRRequestStatus = contractNegotiationService.verifyEDRRequestStatus(assetId);

				downloadResultFields.put("edr", verifyEDRRequestStatus);

				doVerifyResult(assetId, verifyEDRRequestStatus);

				downloadResultFields.put("data", downloadFile(verifyEDRRequestStatus, type));

				downloadResultFields.put(STATUS, "SUCCESS");
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				log.error("We have exception: " + errorMsg);
				prepareErrorMap(downloadResultFields, errorMsg);
			} finally {
				response.put(assetId, downloadResultFields);
			}
		});
		return response;
	}

	private void prepareErrorMap(Map<String, Object> resultFields, String errorMsg) {
		resultFields.put(STATUS, "FAILED");
		resultFields.put("error", errorMsg);
	}

	@SneakyThrows
	private Object downloadFile(EDRCachedResponse verifyEDRRequestStatus, String downloadDataAs) {
		if (verifyEDRRequestStatus != null && NEGOTIATED.equalsIgnoreCase(verifyEDRRequestStatus.getEdrState())) {
			try {
				EDRCachedByIdResponse authorizationToken = contractNegotiationService
						.getAuthorizationTokenForDataDownload(verifyEDRRequestStatus.getTransferProcessId());
				String endpoint = authorizationToken.getEndpoint() + "?type=" + downloadDataAs;
				return edrRequestHelper.getDataFromProvider(authorizationToken, endpoint);
			} catch (FeignException e) {
				log.error("FeignException Download RequestBody: " + e.request());
				String errorMsg = "Unable to download subcribe data offer because: " + e.contentUTF8();
				throw new ServiceException(errorMsg);
			} catch (Exception e) {
				log.error("Exception DownloadFileFromEDCUsingifAlreadyTransferStatusCompleted Oops! We have -"
						+ e.getMessage());
				String errorMsg = "Unable to download subcribe data offer because: " + e.getMessage();
				throw new ServiceException(errorMsg);
			}
		}
		return null;
	}
}
