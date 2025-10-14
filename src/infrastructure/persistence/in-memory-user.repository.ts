import { Injectable } from '@nestjs/common';

import { User } from '../../domain/user/user.aggregate';
import { UserId } from '../../domain/user/user-id.vo';
import { UserRepositoryPort } from '../../application/ports/user.repository.port';

@Injectable()
export class InMemoryUserRepository implements UserRepositoryPort {
  private readonly users = new Map<string, User>();

  async save(user: User): Promise<void> {
    this.users.set(user.getId().toString(), user);
  }

  async findById(id: UserId): Promise<User | null> {
    return this.users.get(id.toString()) ?? null;
  }

  async findByEmail(email: string): Promise<User | null> {
    for (const user of this.users.values()) {
      if (user.getEmail() === email) {
        return user;
      }
    }
    return null;
  }

  async findAll(): Promise<User[]> {
    return Array.from(this.users.values());
  }

  async delete(id: UserId): Promise<void> {
    this.users.delete(id.toString());
  }
}
