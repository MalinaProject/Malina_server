import { Global, Module } from '@nestjs/common';

import { USER_REPOSITORY } from '../application/ports/user.repository.port';
import { UNIT_OF_WORK } from '../application/ports/unit-of-work.port';
import { InMemoryUserRepository } from './persistence/in-memory-user.repository';
import { InMemoryUnitOfWork } from './persistence/in-memory.unit-of-work';

@Global()
@Module({
  providers: [
    InMemoryUserRepository,
    InMemoryUnitOfWork,
    {
      provide: USER_REPOSITORY,
      useExisting: InMemoryUserRepository,
    },
    {
      provide: UNIT_OF_WORK,
      useExisting: InMemoryUnitOfWork,
    },
  ],
  exports: [USER_REPOSITORY, UNIT_OF_WORK],
})
export class InfrastructureModule {}
