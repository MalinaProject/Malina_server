import { Module } from '@nestjs/common';

import { ApplicationModule } from '../../application/application.module';
import { UserCli } from './user.cli';

@Module({
  imports: [ApplicationModule],
  providers: [UserCli],
  exports: [UserCli],
})
export class CliModule {}
