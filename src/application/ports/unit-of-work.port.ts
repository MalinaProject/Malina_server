import { UserRepositoryPort } from './user.repository.port';

export const UNIT_OF_WORK = Symbol('UNIT_OF_WORK');

export interface UnitOfWorkPort {
  execute<T>(work: (repositories: UnitOfWorkRepositories) => Promise<T>): Promise<T>;
}

export interface UnitOfWorkRepositories {
  userRepository: UserRepositoryPort;
}
