/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.edc.entities.request.policies;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDefinitionRequest {

	@JsonProperty("@context")
	@Builder.Default
	private Map<String, String> context = Map.of(
			"@vocab", "https://w3id.org/edc/v0.0.1/ns/",
	        "edc", "https://w3id.org/edc/v0.0.1/ns/",
			"odrl","http://www.w3.org/ns/odrl/2/",
			"tx", "https://w3id.org/tractusx/v0.0.1/ns/",
			"cx-common", "https://w3id.org/catenax/ontology/common#",
	        "cx-taxo", "https://w3id.org/catenax/taxonomy#",
	        "cx-policy", "https://w3id.org/catenax/policy/"
			);

	@JsonProperty("@type")
	@Builder.Default
	private String polityRootType = "PolicyDefinitionRequestDto";

	@JsonProperty("@id")
    private String id;
	
    @JsonProperty("policy")
    private PolicyRequest policyRequest;

    @SneakyThrows
    public String toJsonString() {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
