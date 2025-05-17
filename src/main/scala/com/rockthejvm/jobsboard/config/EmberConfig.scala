package com.rockthejvm.jobsboard.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.derivation.default.*

final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  // need given ConfigReader[Host] and given ConfigReader[Port] so that ConfigReader[EmberConfig] can be generated
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString =>
    Host.fromString(hostString) match {
      case None => Left(CannotConvert(hostString, Host.getClass.toString, s"Invalid host string ${hostString}"))
      case Some(host) => Right(host)
    }
  }

  // rjh note this pattern using "toRight" is equivalent to the more verbose way above
  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port.fromInt(portInt)
      .toRight(CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port number ${portInt}"))
  }

}


