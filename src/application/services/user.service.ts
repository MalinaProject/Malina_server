import { Inject, Injectable } from '@nestjs/common';
import { validateOrReject } from 'class-validator';

import { User } from '../../domain/user/user.aggregate';
import { UserId } from '../../domain/user/user-id.vo';
import { CreateUserCommand } from '../dto/create-user.command';
import { UpdateUserCommand } from '../dto/update-user.command';
import { UserResponseDto, toUserResponse } from '../dto/user.response';
import { UNIT_OF_WORK, UnitOfWorkPort } from '../ports/unit-of-work.port';
import { USER_REPOSITORY, UserRepositoryPort } from '../ports/user.repository.port';

@Injectable()
export class UserService {
  constructor(
    @Inject(UNIT_OF_WORK) private readonly unitOfWork: UnitOfWorkPort,
    @Inject(USER_REPOSITORY) private readonly userRepository: UserRepositoryPort,
  ) {}

  public async createUser(payload: CreateUserCommand): Promise<UserResponseDto> {
    await validateOrReject(payload);

    const existing = await this.userRepository.findByEmail(payload.email);
    if (existing) {
      throw new Error(`User with email ${payload.email} already exists`);
    }

    const user = User.create({
      email: payload.email,
      displayName: payload.displayName,
    });

    await this.unitOfWork.execute(async ({ userRepository }) => {
      await userRepository.save(user);
    });

    return toUserResponse(user);
  }

  public async getUser(id: string): Promise<UserResponseDto> {
    const user = await this.userRepository.findById(UserId.fromString(id));
    if (!user) {
      throw new Error(`User with id ${id} not found`);
    }
    return toUserResponse(user);
  }

  public async listUsers(): Promise<UserResponseDto[]> {
    const users = await this.userRepository.findAll();
    return users.map(toUserResponse);
  }

  public async updateUser(payload: UpdateUserCommand): Promise<UserResponseDto> {
    await validateOrReject(payload);

    const user = await this.userRepository.findById(UserId.fromString(payload.id));
    if (!user) {
      throw new Error(`User with id ${payload.id} not found`);
    }

    await this.unitOfWork.execute(async ({ userRepository }) => {
      if (payload.displayName) {
        user.updateDisplayName(payload.displayName);
      }

      if (payload.email) {
        const duplicate = await userRepository.findByEmail(payload.email);
        if (duplicate && duplicate.getId().toString() !== user.getId().toString()) {
          throw new Error(`User with email ${payload.email} already exists`);
        }
        const restored = User.restore({
          id: user.getId().toString(),
          email: payload.email,
          displayName: user.getDisplayName(),
          createdAt: user.getCreatedAt(),
          updatedAt: new Date(),
        });
        await userRepository.save(restored);
        return;
      }

      await userRepository.save(user);
    });

    return this.getUser(payload.id);
  }

  public async deleteUser(id: string): Promise<void> {
    const userId = UserId.fromString(id);
    const existing = await this.userRepository.findById(userId);
    if (!existing) {
      throw new Error(`User with id ${id} not found`);
    }

    await this.unitOfWork.execute(async ({ userRepository }) => {
      await userRepository.delete(userId);
    });
  }
}
