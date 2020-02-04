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

package uk.gov.hmrc.bobby.domain

import org.joda.time.LocalDate
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class BobbyRulesSpec extends AnyFlatSpec with Matchers {

  "BobbyRules" should "filter plugin and lib dependencies" in {

    val now = new LocalDate()
    val rules: List[BobbyRule] = List(
      BobbyRule(
        Dependency("uk.gov.hmrc", "some-service"),
        VersionRange("(,1.0.0]"),
        "testing",
        now,
        Library),
      BobbyRule(
        Dependency("uk.gov.hmrc", "some-service"),
        VersionRange("(,1.0.0]"),
        "testing",
        now,
        Library),
      BobbyRule(Dependency("uk.gov.hmrc", "some-service"), VersionRange("(,1.0.0]"), "testing", now, Plugin)
    )
    val deps = BobbyRules(rules)

    deps.libs    should be(rules.take(2))
    deps.plugins should be(rules.takeRight(1))
  }

}
