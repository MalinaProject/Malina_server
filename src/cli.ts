import 'reflect-metadata';

import { ValidationError } from 'class-validator';
import { NestFactory } from '@nestjs/core';

import { AppModule } from './app.module';
import { UserCli } from './infrastructure/cli/user.cli';

async function bootstrap(): Promise<void> {
  const appContext = await NestFactory.createApplicationContext(AppModule, {
    logger: false,
  });

  try {
    const cli = appContext.get(UserCli);
    await cli.execute(process.argv.slice(2));
  } catch (error) {
    console.error(JSON.stringify({ error: serializeError(error) }, null, 2));
    process.exitCode = 1;
  } finally {
    await appContext.close();
  }
}

void bootstrap();

function serializeError(error: unknown): unknown {
  if (
    Array.isArray(error) &&
    error.every((element) => element instanceof ValidationError)
  ) {
    return error.map((element) => ({
      property: element.property,
      constraints: element.constraints,
    }));
  }

  if (error instanceof Error) {
    return { message: error.message };
  }

  return { message: String(error) };
}
