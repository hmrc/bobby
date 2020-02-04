/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bobby.conf

import java.io.File

import sbt.ConsoleLogger

import scala.io.Source

class ConfigFile(fileName: String) {
  val logger = ConsoleLogger()

  if (!new File(fileName).exists()) {
    logger.warn(s"Supplied configuration file '$fileName' does not exist.")
  }

  val load: Map[String, String] = {
    try {
      val source = Source.fromFile(fileName)
      val lines = source.getLines().toList
      source.close()
      Configuration.extractMap(lines)
    } catch {
      case e: Exception =>
        logger.debug(s"[bobby] Unable to find $fileName. ${e.getClass.getName}: ${e.getMessage}")
        Map.empty
    }
  }

  def get(path: String): Option[String] = load.get(path)

}
