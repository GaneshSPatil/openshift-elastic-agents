/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package cd.go.contrib.elasticagent.executors;

import cd.go.contrib.elasticagent.KubernetesClientFactory;
import cd.go.contrib.elasticagent.PluginRequest;
import cd.go.contrib.elasticagent.builders.PluginStatusReportViewBuilder;
import cd.go.contrib.elasticagent.reports.StatusReportGenerationErrorHandler;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.net.URL;

import static cd.go.contrib.elasticagent.KubernetesPlugin.LOG;

public class StatusReportExecutor {
    private final PluginRequest pluginRequest;
    private final KubernetesClientFactory factory;
    private final PluginStatusReportViewBuilder statusReportViewBuilder;


    public StatusReportExecutor(PluginRequest pluginRequest) {
        this(pluginRequest, KubernetesClientFactory.instance(), PluginStatusReportViewBuilder.instance());
    }

    public StatusReportExecutor(PluginRequest pluginRequest, KubernetesClientFactory factory, PluginStatusReportViewBuilder statusReportViewBuilder) {
        this.pluginRequest = pluginRequest;
        this.factory = factory;
        this.statusReportViewBuilder = statusReportViewBuilder;
    }

    public GoPluginApiResponse execute() {
        try {
            LOG.info("[status-report] Generating status report.");
            KubernetesClient client = factory.client(pluginRequest.getPluginSettings());
            String projectOverviewUrl = new URL(client.getMasterUrl(), "console/project/" + client.getNamespace() + "/browse/pods").toString();
            LOG.info(String.format("Redirecting user to %s", projectOverviewUrl));

//            final KubernetesCluster kubernetesCluster = new KubernetesCluster(client);
//            final Template template = statusReportViewBuilder.getTemplate("status-report.template.ftlh");
//            final String statusReportView = statusReportViewBuilder.build(template, kubernetesCluster);

            final String statusReportView = "<meta http-equiv=\"refresh\" content=\"0; url=" + projectOverviewUrl + "\" />";
            final JsonObject responseJSON = new JsonObject();
            responseJSON.addProperty("view", statusReportView);

            return DefaultGoPluginApiResponse.success(responseJSON.toString());
        } catch (Exception e) {
            return StatusReportGenerationErrorHandler.handle(statusReportViewBuilder, e);
        }
    }
}
