package bio.terra.policy.common.exception;

import bio.terra.common.exception.BadRequestException;

public class IllegalCycleException extends BadRequestException {
  public IllegalCycleException(String message) {
    super(message);
  }
}
