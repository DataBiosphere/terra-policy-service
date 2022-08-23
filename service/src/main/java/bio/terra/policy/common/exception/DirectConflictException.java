package bio.terra.policy.common.exception;

import bio.terra.common.exception.NotFoundException;

public class DirectConflictException extends NotFoundException {
  public DirectConflictException(String message) {
    super(message);
  }

  public DirectConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
