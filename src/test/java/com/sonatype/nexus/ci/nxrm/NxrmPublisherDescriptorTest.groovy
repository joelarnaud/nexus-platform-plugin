/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.ApiStub.NexusClientFactory
import com.sonatype.nexus.api.ApiStub.NxrmClient
import com.sonatype.nexus.ci.config.GlobalNexusConfiguration
import com.sonatype.nexus.ci.config.Nxrm2Configuration
import com.sonatype.nexus.ci.config.NxrmConfiguration
import com.sonatype.nexus.ci.util.FormUtil

import groovy.mock.interceptor.MockFor
import hudson.model.Describable
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

abstract class NxrmPublisherDescriptorTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  abstract Class<? extends Describable> getDescribable()

  def 'it populates Nexus instances'() {
    setup:
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()

    when: 'nexus instance items are filled'
      def configuration = (NxrmPublisherDescriptor) jenkins.getInstance().getDescriptor(describable)
      def listBoxModel = configuration.doFillNexusInstanceIdItems()

    then: 'ListBox has the correct size'
      listBoxModel.size() == 2

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == nxrm2Configuration.displayName
      listBoxModel.get(1).value == nxrm2Configuration.internalId
  }

  def 'it populates Nexus repositories'() {
    setup:
      GroovyMock(NexusClientFactory.class, global: true)
      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()

      def client = new MockFor(NxrmClient)
      def repositories = [
          [
              id  : 'maven-releases',
              name: 'Maven Releases'
          ],
          [
              id  : 'nuget-releases',
              name: 'NuGet Releases'
          ]
      ]
      client.demand.getNxrmRepositories { repositories }
      NexusClientFactory.buildRmClient(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> new NxrmClient()

    when: 'nexus repository items are filled'
      def configuration = (NxrmPublisherDescriptor) jenkins.getInstance().getDescriptor(describable)
      def listBoxModel
      client.use {
        listBoxModel = configuration.doFillNexusRepositoryIdItems(nxrm2Configuration.internalId)
      }

    then: 'ListBox has the correct size'
      listBoxModel.size() == 3

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == repositories.get(0).name
      listBoxModel.get(1).value == repositories.get(0).id
      listBoxModel.get(2).name == repositories.get(1).name
      listBoxModel.get(2).value == repositories.get(1).id
  }

  protected Nxrm2Configuration saveGlobalConfigurationWithNxrm2Configuration() {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm2Configuration = new Nxrm2Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credentialsId')
    configurationList.push(nxrm2Configuration)

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration.class)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    return nxrm2Configuration
  }
}