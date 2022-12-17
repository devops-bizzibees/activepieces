package com.activepieces.flow.validator;

import com.activepieces.common.model.ArtifactFile;
import com.activepieces.common.error.ErrorServiceHandler;
import com.activepieces.common.error.exception.flow.FlowNotFoundException;
import com.activepieces.component.ComponentService;
import com.activepieces.entity.enums.ResourceType;
import com.activepieces.flow.FlowService;
import com.activepieces.flow.model.FlowVersionView;
import com.activepieces.flow.validator.constraints.*;
import com.activepieces.guardian.client.PermissionService;
import com.activepieces.guardian.client.exception.PermissionDeniedException;
import com.github.ksuid.Ksuid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.List;
import java.util.Objects;

@Service
public class FlowVersionValidator {

  private final PermissionService permissionService;
  private final UniqueStepsNameValidator uniqueStepsNameValidator;
  private final CodeArtifactsValidator requiredCodeArtifactsValidator;
  private final StepsValidator stepsValidator;
  private final FlowValidValidator flowValidValidator;
  private final FlowService flowService;
  private final ErrorServiceHandler errorServiceHandler;

  @Autowired
  public FlowVersionValidator(
      PermissionService permissionService,
      Validator validator,
      ComponentService componentService,
      ErrorServiceHandler errorServiceHandler,
      FlowService flowService) {
    this.errorServiceHandler = errorServiceHandler;
    this.flowService = flowService;
    this.permissionService = permissionService;
    this.uniqueStepsNameValidator = new UniqueStepsNameValidator();
    this.flowValidValidator = new FlowValidValidator();
    this.stepsValidator = new StepsValidator(validator, componentService);
    this.requiredCodeArtifactsValidator = new CodeArtifactsValidator();
  }

  public FlowVersionView constructRequest(
          Ksuid collectionId, Ksuid flowId, FlowVersionView request, List<ArtifactFile> files)
          throws Exception {
    if (Objects.isNull(collectionId)) {
      collectionId =
          permissionService
              .getFirstResourceParentWithType(flowId, ResourceType.COLLECTION)
              .getResourceId();
    }
    Ksuid projectId =
        permissionService
            .getFirstResourceParentWithType(collectionId, ResourceType.PROJECT)
            .getResourceId();

    FlowVersionView draftVersion = getDraftVersion(flowId);
    List<FlowVersionRequestBuilder> flowVersionRequestBuilders =
        List.of(
            uniqueStepsNameValidator,
            requiredCodeArtifactsValidator,
            stepsValidator,
            flowValidValidator);
    FlowVersionView finalVersion = request.toBuilder().build();
    for (FlowVersionRequestBuilder flowVersionRequestBuilder : flowVersionRequestBuilders) {
      finalVersion =
          flowVersionRequestBuilder.construct(
              projectId, collectionId, finalVersion, files, draftVersion);
    }

    return finalVersion;
  }

  private FlowVersionView getDraftVersion(Ksuid flowId) {
    if (Objects.nonNull(flowId)) {
      try {
        return flowService.get(flowId).getLastVersion();
      } catch (FlowNotFoundException | PermissionDeniedException e) {
        throw errorServiceHandler.createInternalError(e);
      }
    }
    return null;
  }
}
