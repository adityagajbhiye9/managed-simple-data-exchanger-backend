/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
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

package org.eclipse.tractusx.sde.edc.entities.request.asset;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AssetEntryRequest {

	@JsonProperty("@context")
	@Builder.Default
	private Map<String,String> context = Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/",
	        "oauth2", "https://datatracker.ietf.org/doc/html/rfc6749",
	        "dcat", "https://www.w3.org/ns/dcat/",
	        "odrl", "http://www.w3.org/ ns/odrl/2/",
	        "dct", "http://purl.org/dc/terms/",
	        "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
	        "cx-taxo",  "https://w3id.org/catenax/taxonomy#",
	        "cx-common", "https://w3id.org/catenax/ontology/common#",
	        "aas-semantics", "https://admin-shell.io/aas/3/0/HasSemantics/");

	@JsonProperty("@id")
	private String id;
	private HashMap<String, Object> properties;
	private DataAddressRequest dataAddress;
	

	@SneakyThrows
	public String toJsonString() {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}
