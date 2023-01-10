package bio.terra.policy.common.exception;

import bio.terra.common.exception.ConflictException;

public class DirectConflictException extends ConflictException {
  public DirectConflictException(String message) {
    super(message);
  }

  public DirectConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
