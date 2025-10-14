import { Inject, Injectable } from '@nestjs/common';

import { UnitOfWorkPort, UnitOfWorkRepositories } from '../../application/ports/unit-of-work.port';
import { USER_REPOSITORY, UserRepositoryPort } from '../../application/ports/user.repository.port';

@Injectable()
export class InMemoryUnitOfWork implements UnitOfWorkPort {
  constructor(@Inject(USER_REPOSITORY) private readonly userRepository: UserRepositoryPort) {}

  async execute<T>(work: (repositories: UnitOfWorkRepositories) => Promise<T>): Promise<T> {
    return work({ userRepository: this.userRepository });
  }
}
