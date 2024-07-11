package bio.terra.policy.db.exception;

import bio.terra.common.exception.ConflictException;

public class DuplicateObjectException extends ConflictException {
  public DuplicateObjectException(String message) {
    super(message);
  }
}
