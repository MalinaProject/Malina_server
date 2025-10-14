import { Injectable } from '@nestjs/common';
import { promises as fs } from 'fs';
import * as path from 'path';
import yargs from 'yargs';

import { CreateUserCommand } from '../../application/dto/create-user.command';
import { UpdateUserCommand } from '../../application/dto/update-user.command';
import { UserService } from '../../application/services/user.service';

@Injectable()
export class UserCli {
  constructor(private readonly userService: UserService) {}

  async execute(argv: string[]): Promise<void> {
    const parser = yargs(argv)
      .scriptName('user-cli')
      .command(
        'create',
        'Create a user',
        (y) =>
          y
            .option('email', {
              type: 'string',
              demandOption: true,
              describe: 'Email of the user',
            })
            .option('displayName', {
              type: 'string',
              demandOption: true,
              describe: 'Display name',
            })
            .option('output', {
              type: 'string',
              default: 'output.json',
              describe: 'Path to write JSON response',
            }),
        async (args) => {
          const command = new CreateUserCommand({
            email: args.email,
            displayName: args.displayName,
          });
          const result = await this.userService.createUser(command);
          await this.writeOutput(args.output as string, result);
        },
      )
      .command(
        'get',
        'Get user by id',
        (y) =>
          y
            .option('id', {
              type: 'string',
              demandOption: true,
              describe: 'User identifier',
            })
            .option('output', {
              type: 'string',
              default: 'output.json',
            }),
        async (args) => {
          const result = await this.userService.getUser(args.id as string);
          await this.writeOutput(args.output as string, result);
        },
      )
      .command(
        'list',
        'List users',
        (y) =>
          y.option('output', {
            type: 'string',
            default: 'output.json',
          }),
        async (args) => {
          const result = await this.userService.listUsers();
          await this.writeOutput(args.output as string, result);
        },
      )
      .command(
        'update',
        'Update user',
        (y) =>
          y
            .option('id', {
              type: 'string',
              demandOption: true,
            })
            .option('email', {
              type: 'string',
            })
            .option('displayName', {
              type: 'string',
            })
            .option('output', {
              type: 'string',
              default: 'output.json',
            }),
        async (args) => {
          const command = new UpdateUserCommand({
            id: args.id as string,
            email: args.email as string | undefined,
            displayName: args.displayName as string | undefined,
          });
          const result = await this.userService.updateUser(command);
          await this.writeOutput(args.output as string, result);
        },
      )
      .command(
        'delete',
        'Delete user',
        (y) =>
          y
            .option('id', {
              type: 'string',
              demandOption: true,
            })
            .option('output', {
              type: 'string',
              default: 'output.json',
            }),
        async (args) => {
          await this.userService.deleteUser(args.id as string);
          await this.writeOutput(args.output as string, { success: true });
        },
      )
      .demandCommand(1)
      .strict();

    await parser.parseAsync();
  }

  private async writeOutput(outputPath: string, data: unknown): Promise<void> {
    const absolute = path.isAbsolute(outputPath)
      ? outputPath
      : path.join(process.cwd(), outputPath);
    const json = JSON.stringify(data, null, 2);
    await fs.mkdir(path.dirname(absolute), { recursive: true });
    await fs.writeFile(absolute, json, 'utf-8');
    console.log(json);
  }
}
