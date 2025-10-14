import { Module } from '@nestjs/common';

import { ApplicationModule } from './application/application.module';
import { CliModule } from './infrastructure/cli/cli.module';
import { InfrastructureModule } from './infrastructure/infrastructure.module';

@Module({
  imports: [InfrastructureModule, ApplicationModule, CliModule],
})
export class AppModule {}
