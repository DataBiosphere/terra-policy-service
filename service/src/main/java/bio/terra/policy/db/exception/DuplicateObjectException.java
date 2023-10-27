package bio.terra.policy.db.exception;

import bio.terra.common.exception.BadRequestException;

public class DuplicateObjectException extends ConflictException {
  public DuplicateObjectException(String message) {
    super(message);
  }
}
