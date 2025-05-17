package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PostgresConfig(url: String, user: String, password: String, nthreads: Int) 
  derives ConfigReader
