/********************************************************************************
 * Copyright (c) 2023 T-Systems International GmbH
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.configuration;

import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.edc.facilitator.CreateEDCAssetFacilator;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("default")
public class DigitalTwinAssetProvider {

	private final AssetEntryRequestFactory assetFactory;
	private final EDCGateway edcGateway;
	private final CreateEDCAssetFacilator createEDCAssetFacilator;

	@Value("${digital-twins.hostname:default}${digital-twins.api:/api/v3.0}")
	private String digitalTwinRegistry;

	@PostConstruct
	public void init() {
		AssetEntryRequest assetEntryRequest = assetFactory.getAssetRequest("", "Digital twin registry information",
				"digitaltwin", "asset_id", "");
		assetEntryRequest.getAsset().getProperties().put("type", "data.core.digitalTwinRegistry");
		assetEntryRequest.getDataAddress().getProperties().put("baseUrl", digitalTwinRegistry);
		assetEntryRequest.getDataAddress().getProperties().remove("oauth2:tokenUrl");
		assetEntryRequest.getDataAddress().getProperties().remove("oauth2:clientId");
		assetEntryRequest.getDataAddress().getProperties().remove("oauth2:clientSecretKey");
		
		if (!edcGateway.assetExistsLookup(assetEntryRequest.getAsset().getId())) {
			Map<String, String> createEDCAsset = createEDCAssetFacilator.createEDCAsset(assetEntryRequest, List.of(), Map.of());
			log.info("Digital twin asset creates :"+createEDCAsset.toString());
		}else {
			log.info("Digital twin asset exists in edc connector, so ignoring asset creation");
		}
	}

}
