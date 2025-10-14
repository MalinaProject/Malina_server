import { IsEmail, IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class CreateUserCommand {
  @IsEmail()
  email!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(50)
  displayName!: string;

  constructor(params: Partial<CreateUserCommand> = {}) {
    Object.assign(this, params);
  }
}
