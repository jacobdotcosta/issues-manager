package dev.snowdrop.jira.atlassian;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import com.beust.jcommander.JCommander;

import org.jboss.logging.Logger;

import static dev.snowdrop.jira.atlassian.Utility.*;

public class Client {
    private static final Logger LOG = Logger.getLogger(Client.class);
    private static final String TARGET_RELEASE_CUSTOMFIELD_ID = "customfield_12311240";
    private static Args args;
    private static JiraRestClient restClient;

    public static void main(String[] argv) {
        Client client = new Client();
        args = new Args();

        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        client.init();

        switch(args.action) {
            case "get" :
                client.getIssue(args.issue);
                break;

            case "create" :
                client.createIssue();
                break;

            case "delete" :
                client.deleteIssue(args.issue);
                break;
        }
    }

    private void init() {
        try {
            // Parse YAML config
            readYaml(args.cfg);

            // Create JIRA authenticated client
            AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
            restClient = factory.createWithBasicHttpAuthentication(jiraServerUri(args.jiraServerUri), args.user, args.password);
            restClient.getSessionClient().getCurrentSession().get().getLoginInfo().getFailedLoginCount();

        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private void getIssue(String issueNumber) {
        final IssueRestClient cl = restClient.getIssueClient();
        // Get Issue Info
        Issue issue = cl.getIssue(issueNumber).claim();
        println(issue);
    }

    private void deleteIssue(String issue) {
        final IssueRestClient cl = restClient.getIssueClient();
        cl.deleteIssue(issue, false);
        LOG.infof("Issue %s deleted",issue);
    }

    private void createIssue() {
        final IssueRestClient cl = restClient.getIssueClient();

        IssueInputBuilder iib = new IssueInputBuilder();
        iib.setProjectKey("ENTSBT");
        iib.setSummary(release.getTitle());
        iib.setDescription(
                String.format(release.getTemplate(),
                        release.getVersion(),
                        release.getDate(),
                        release.getEOL()));
        iib.setIssueType(TASK_TYPE());
        iib.setDueDate(formatDueDate(release.getDueDate()));
        // See: https://github.com/snowdrop/jira-tool/issues/7
        // iib.setFixVersions(setFixVersion());
        /*
         * {
         *   id=customfield_12311240,
         *   name=Target
         *   Release,
         *   type=null,
         *   value=
         * {
         *   "self": "https:\/\/issues.redhat.com\/rest\/api\/2\/version\/12345960",
         *   "id": "12345960",
         *   "description": "Spring Boot 2.3 Release",
         *   "name": "2.3.0.GA",
         *   "archived": false,
         *   "released": false,
         *   "releaseDate": "2020-09-14"
         * }
         */
        iib.setFieldValue(TARGET_RELEASE_CUSTOMFIELD_ID,setTargetRelease());

        IssueInput issue = iib.build();
        BasicIssue issueObj = cl.createIssue(issue).claim();
        LOG.infof("Issue %s created successfully",issueObj.getKey());
    }

    private void println(Object o) {
        System.out.println(o);
    }
}