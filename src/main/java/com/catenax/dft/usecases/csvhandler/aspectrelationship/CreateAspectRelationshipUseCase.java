/*
 * Copyright 2022 CatenaX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.catenax.dft.usecases.csvhandler.aspectrelationship;

import com.catenax.dft.entities.aspectrelationship.AspectRelationshipRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CreateAspectRelationshipUseCase {
    private final MapFromAspectRelationshipRequestUseCase useCase;

    public CreateAspectRelationshipUseCase(MapFromAspectRelationshipRequestUseCase useCase) {
        this.useCase = useCase;
    }

    public void createAspects(List<AspectRelationshipRequest> aspects, String processId){
        for(int i=0; i<aspects.size();i++){
            AspectRelationshipRequest aspect = aspects.get(i);
            aspect.setRowNumber(i);
            aspect.setProcessId(processId);
            useCase.run(aspect, processId);
        }
    }

}
