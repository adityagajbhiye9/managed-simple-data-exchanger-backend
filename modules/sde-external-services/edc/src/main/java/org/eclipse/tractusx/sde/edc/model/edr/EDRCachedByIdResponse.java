/********************************************************************************
 * Copyright (c) 2023, 2024 T-Systems International GmbH
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.edc.model.edr;

import org.eclipse.tractusx.sde.edc.constants.EDCAssetConstant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EDRCachedByIdResponse {

	@JsonProperty(EDCAssetConstant.ASSET_PREFIX + "type")
	private String type;

	@JsonProperty(EDCAssetConstant.ASSET_PREFIX + "authorization")
	private String authorization;

	@JsonProperty(EDCAssetConstant.ASSET_PREFIX + "endpoint")
	private String endpoint;

	@JsonProperty(EDCAssetConstant.TX_AUTH_PREFIX + "refreshEndpoint")
	private String refreshEndpoint;
	
	@JsonProperty(EDCAssetConstant.TX_AUTH_PREFIX + "audience")
	private String audience;
	
	@JsonProperty(EDCAssetConstant.TX_AUTH_PREFIX + "refreshToken")
	private String refreshToken;
	
	@JsonProperty(EDCAssetConstant.TX_AUTH_PREFIX + "expiresIn")
	private String expiresIn;

	@JsonProperty(EDCAssetConstant.ASSET_PREFIX + "refreshAudience")
	private String refreshAudience;

}