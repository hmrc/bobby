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

package uk.gov.hmrc.bobby

import java.net.URL

import sbt._
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.Repo
import uk.gov.hmrc.bobby.conf.Configuration
import uk.gov.hmrc.bobby.domain._
import uk.gov.hmrc.bobby.output.Output
import uk.gov.hmrc.bobby.repos.Repositories

class BobbyValidationFailedException(message: String) extends RuntimeException(message)

object Bobby {

  private val logger         = ConsoleLogger()
  private val currentVersion = getClass.getPackage.getImplementationVersion

  val ignoredOrgs = Set(
    "com.typesafe.play",
    "com.kenshoo",
    "com.codahale.metrics",
    "org.scala-lang"
  )

  def validateDependencies(
    projectDependencyMap: Map[ModuleID, Seq[ModuleID]],
    projectDependencies: Seq[ModuleID],
    pluginDependencies: Seq[ModuleID],
    scalaVersion: String,
    reposValue: Seq[Repo],
    checkForLatest: Boolean,
    bobbyRulesUrl: Option[URL] = None,
    jsonOutputFileOverride: Option[String] = None): Unit = {

    logger.info(s"[bobby] Bobby version $currentVersion")

    val config = new Configuration(bobbyRulesUrl, jsonOutputFileOverride)

    val filteredLibraries = filterDependencies(projectDependencies, ignoredOrgs)

    val latestDiscoveredVersions = if (checkForLatest) {
      findLatestVersions(scalaVersion, reposValue, filteredLibraries++pluginDependencies)
    } else Map.empty[ModuleID, Option[Version]]

    val messages =
      BobbyValidator.applyBobbyRules(projectDependencyMap, filteredLibraries, pluginDependencies, latestDiscoveredVersions, config.loadBobbyRules)

    Output.outputMessagesToConsole(messages)
    Output.writeMessagesToFile(messages, config.jsonOutputFile, config.textOutputFile)

    if (messages.exists(_.isError))
      throw new BobbyValidationFailedException("See previous bobby output for more information")
  }

  def findLatestVersions(
    scalaVersion: String,
    repositoriesToCheck: Seq[Repo],
    prepared: Seq[ModuleID]): Map[ModuleID, Option[Version]] = {
    val repoSearch = Repositories.buildAggregateRepositories(repositoriesToCheck)
    getLatestRepoRevisions(scalaVersion, prepared, repoSearch)
  }

  private[bobby] def filterDependencies(dependencies: Seq[ModuleID], ignoreList: Set[String]): Seq[ModuleID] =
    compactDependencies(dependencies)
      .filterNot(m => ignoreList.contains(m.organization))

  private[bobby] def getLatestRepoRevisions(
    scalaVersion: String,
    compacted: Seq[ModuleID],
    repoSearch: RepoSearch
  ): Map[ModuleID, Option[Version]] =
    compacted.par
      .map { module =>
        module -> repoSearch.findLatestRevision(module, Option(scalaVersion))
      }
      .seq
      .toMap.mapValues(_.toOption)

  private[bobby] def compactDependencies(dependencies: Seq[ModuleID]): Seq[ModuleID] = {
    def orgAndName(d: ModuleID) = s"${d.organization}.${d.name}"

    dependencies
      .groupBy(orgAndName)
      .map(_._2.head)
      .toSeq
  }

}
