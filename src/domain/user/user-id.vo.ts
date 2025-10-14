import { randomUUID } from 'crypto';

export class UserId {
  private constructor(private readonly value: string) {}

  public static generate(): UserId {
    return new UserId(randomUUID());
  }

  public static fromString(value: string): UserId {
    if (!value) {
      throw new Error('UserId cannot be empty');
    }
    return new UserId(value);
  }

  public toString(): string {
    return this.value;
  }
}
