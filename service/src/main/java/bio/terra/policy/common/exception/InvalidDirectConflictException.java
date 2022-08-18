package bio.terra.policy.common.exception;

import bio.terra.common.exception.NotFoundException;

public class InvalidDirectConflictException extends NotFoundException {
  public InvalidDirectConflictException(String message) {
    super(message);
  }

  public InvalidDirectConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
