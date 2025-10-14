import { IsEmail, IsOptional, IsString, MaxLength } from 'class-validator';

export class UpdateUserCommand {
  @IsString()
  id!: string;

  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  @MaxLength(50)
  displayName?: string;

  constructor(params: Partial<UpdateUserCommand> = {}) {
    Object.assign(this, params);
  }
}
