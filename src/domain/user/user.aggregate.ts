import { UserId } from './user-id.vo';

export interface UserProps {
  readonly email: string;
  readonly displayName: string;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export class User {
  private constructor(
    private readonly id: UserId,
    private props: UserProps,
  ) {}

  public static create(params: { email: string; displayName: string }): User {
    const now = new Date();
    return new User(UserId.generate(), {
      email: params.email,
      displayName: params.displayName,
      createdAt: now,
      updatedAt: now,
    });
  }

  public static restore(props: { id: string } & UserProps): User {
    return new User(UserId.fromString(props.id), props);
  }

  public getId(): UserId {
    return this.id;
  }

  public getEmail(): string {
    return this.props.email;
  }

  public getDisplayName(): string {
    return this.props.displayName;
  }

  public getCreatedAt(): Date {
    return this.props.createdAt;
  }

  public getUpdatedAt(): Date {
    return this.props.updatedAt;
  }

  public updateDisplayName(displayName: string): void {
    this.props = {
      ...this.props,
      displayName,
      updatedAt: new Date(),
    };
  }

  public toPrimitives(): Record<string, unknown> {
    return {
      id: this.id.toString(),
      email: this.props.email,
      displayName: this.props.displayName,
      createdAt: this.props.createdAt.toISOString(),
      updatedAt: this.props.updatedAt.toISOString(),
    };
  }
}
