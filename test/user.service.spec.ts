import { CreateUserCommand } from '../src/application/dto/create-user.command';
import { UpdateUserCommand } from '../src/application/dto/update-user.command';
import { UserService } from '../src/application/services/user.service';
import { UserId } from '../src/domain/user/user-id.vo';
import { InMemoryUnitOfWork } from '../src/infrastructure/persistence/in-memory.unit-of-work';
import { InMemoryUserRepository } from '../src/infrastructure/persistence/in-memory-user.repository';

describe('UserService', () => {
  let repository: InMemoryUserRepository;
  let unitOfWork: InMemoryUnitOfWork;
  let service: UserService;

  beforeEach(() => {
    repository = new InMemoryUserRepository();
    unitOfWork = new InMemoryUnitOfWork(repository);
    service = new UserService(unitOfWork, repository);
  });

  it('creates a user and persists it', async () => {
    const command = new CreateUserCommand({
      email: 'user@example.com',
      displayName: 'User Name',
    });

    const result = await service.createUser(command);

    expect(result).toMatchObject({
      email: 'user@example.com',
      displayName: 'User Name',
    });
    expect(result.id).toBeDefined();

    const stored = await repository.findById(UserId.fromString(result.id));
    expect(stored?.getEmail()).toBe('user@example.com');
    expect(stored?.getDisplayName()).toBe('User Name');
  });

  it('prevents creating two users with the same email', async () => {
    const command = new CreateUserCommand({
      email: 'duplicate@example.com',
      displayName: 'First',
    });
    await service.createUser(command);

    await expect(
      service.createUser(
        new CreateUserCommand({
          email: 'duplicate@example.com',
          displayName: 'Second',
        }),
      ),
    ).rejects.toThrow('User with email duplicate@example.com already exists');
  });

  it('validates user input when creating', async () => {
    const invalid = new CreateUserCommand({
      email: 'not-an-email',
      displayName: '',
    });

    await expect(service.createUser(invalid)).rejects.toEqual(
      expect.arrayContaining([
        expect.objectContaining({ property: 'email' }),
        expect.objectContaining({ property: 'displayName' }),
      ]),
    );
  });

  it('updates user attributes via unit of work', async () => {
    const created = await service.createUser(
      new CreateUserCommand({
        email: 'change@example.com',
        displayName: 'Before',
      }),
    );

    const updated = await service.updateUser(
      new UpdateUserCommand({
        id: created.id,
        displayName: 'After',
      }),
    );

    expect(updated.displayName).toBe('After');
    const stored = await repository.findById(UserId.fromString(created.id));
    expect(stored?.getDisplayName()).toBe('After');
  });

  it('deletes users through the repository', async () => {
    const created = await service.createUser(
      new CreateUserCommand({
        email: 'delete@example.com',
        displayName: 'ToRemove',
      }),
    );

    await service.deleteUser(created.id);

    await expect(service.getUser(created.id)).rejects.toThrow(
      `User with id ${created.id} not found`,
    );
  });
});
