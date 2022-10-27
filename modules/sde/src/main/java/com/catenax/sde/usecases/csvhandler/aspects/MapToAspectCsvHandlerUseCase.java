/********************************************************************************
 * Copyright (c) 2022 Critical TechWorks GmbH
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022 T-Systems International GmbH
 * Copyright (c) 2022 Contributors to the CatenaX (ng) GitHub Organisation
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

package com.catenax.sde.usecases.csvhandler.aspects;

import static com.catenax.sde.gateways.file.CsvGateway.SEPARATOR;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.catenax.sde.entities.SubmodelFileRequest;
import com.catenax.sde.entities.csv.RowData;
import com.catenax.sde.entities.usecases.Aspect;
import com.catenax.sde.usecases.common.DftDateValidator;
import com.catenax.sde.usecases.csvhandler.AbstractCsvHandlerUseCase;
import com.catenax.sde.usecases.csvhandler.CsvHandlerOrchestrator;
import com.catenax.sde.usecases.csvhandler.exceptions.CsvHandlerUseCaseException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MapToAspectCsvHandlerUseCase extends AbstractCsvHandlerUseCase<RowData, Aspect> {

	private SubmodelFileRequest submodelFileRequest;
	
	@Autowired
	private DftDateValidator dftDateValidator;
	
    public MapToAspectCsvHandlerUseCase(GenerateAspectUuIdCsvHandlerUseCase nextUseCase) {
        super(nextUseCase);
    }
    
    public void init(SubmodelFileRequest submodelFileRequest) {
		this.submodelFileRequest = submodelFileRequest;
	}

    @SneakyThrows
    public Aspect executeUseCase(RowData rowData, String processId) {
        String[] rowDataFields = rowData.content().split(SEPARATOR, -1);
        if (rowDataFields.length != CsvHandlerOrchestrator.getAspectColumnSize()) {
            throw new CsvHandlerUseCaseException(rowData.position(), "This row has the wrong amount of fields");
        }

        Aspect aspect = Aspect.builder()
                .rowNumber(rowData.position())
                .uuid(rowDataFields[0].trim())
                .processId(processId)
                .partInstanceId(rowDataFields[1].trim())
                .manufacturingDate(dftDateValidator.getIfValidDateTime(rowDataFields[2].trim(),rowData.position()))
                .manufacturingCountry(rowDataFields[3].trim().isBlank() ? null : rowDataFields[3])
                .manufacturerPartId(rowDataFields[4].trim())
                .customerPartId(rowDataFields[5].trim().isBlank() ? null : rowDataFields[5])
                .classification(rowDataFields[6].trim())
                .nameAtManufacturer(rowDataFields[7].trim())
                .nameAtCustomer(rowDataFields[8].trim().isBlank() ? null : rowDataFields[8])
                .optionalIdentifierKey(rowDataFields[9].isBlank() ? null : rowDataFields[9])
                .optionalIdentifierValue(rowDataFields[10].isBlank() ? null : rowDataFields[10])
                .build();

        List<String> errorMessages = validateAsset(aspect);
        if (!errorMessages.isEmpty()) {
            throw new CsvHandlerUseCaseException(rowData.position(), errorMessages.toString());
        }

        aspect.setBpnNumbers(this.submodelFileRequest.getBpnNumbers());
        aspect.setUsagePolicies(submodelFileRequest.getUsagePolicies());
        aspect.setTypeOfAccess(submodelFileRequest.getTypeOfAccess());
        
        return aspect;
    }

    private List<String> validateAsset(Aspect asset) {
        Validator validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
        Set<ConstraintViolation<Aspect>> violations = validator.validate(asset);

        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();
    }

	
}