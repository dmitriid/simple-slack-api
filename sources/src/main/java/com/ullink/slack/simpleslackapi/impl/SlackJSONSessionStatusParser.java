package com.ullink.slack.simpleslackapi.impl;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPersona;
import com.ullink.slack.simpleslackapi.SlackTeam;
import com.ullink.slack.simpleslackapi.SlackUser;

class SlackJSONSessionStatusParser
{
    private static final Logger       LOGGER   = LoggerFactory.getLogger(SlackJSONSessionStatusParser.class);

    private Map<String, SlackChannel> channels = new HashMap<>();
    private Map<String, SlackUser>    users    = new HashMap<>();

    private SlackPersona              sessionPersona;

    private SlackTeam                 team;

    private String                    webSocketURL;

    private String                    toParse;

    private String                    error;

    SlackJSONSessionStatusParser(String toParse)
    {
        this.toParse = toParse;
    }

    Map<String, SlackChannel> getChannels()
    {
        return channels;
    }

    Map<String, SlackUser> getUsers()
    {
        return users;
    }

    public String getWebSocketURL()
    {
        return webSocketURL;
    }

    public String getError()
    {
        return error;
    }
    
    void parse()
    {
        LOGGER.debug("parsing session status : " + toParse);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(toParse).getAsJsonObject();
        Boolean ok = jsonResponse.get("ok").getAsBoolean();
        if (Boolean.FALSE.equals(ok)) {
            error = (String)jsonResponse.get("error").getAsString();
            return;
        }
        JsonArray usersJson = jsonResponse.get("users").getAsJsonArray();

        for (JsonElement jsonObject : usersJson)
        {
            JsonObject jsonUser = jsonObject.getAsJsonObject();
            SlackUser slackUser = SlackJSONParsingUtils.buildSlackUser(jsonUser);
            LOGGER.debug("slack user found : " + slackUser.getId());
            users.put(slackUser.getId(), slackUser);
        }

        if (jsonResponse.get("bots") != null) {
            JsonArray botsJson = jsonResponse.get("bots").getAsJsonArray();
            for (JsonElement jsonObject : botsJson)
            {
                JsonObject jsonBot = jsonObject.getAsJsonObject();
                SlackUser slackUser = SlackJSONParsingUtils.buildSlackUser(jsonBot);
                LOGGER.debug("slack bot found : " + slackUser.getId());
                users.put(slackUser.getId(), slackUser);
            }
        }

        JsonArray channelsJson = jsonResponse.get("channels").getAsJsonArray();

        for (JsonElement jsonObject : channelsJson)
        {
            JsonObject jsonChannel = jsonObject.getAsJsonObject();
            SlackChannelImpl channel = SlackJSONParsingUtils.buildSlackChannel(jsonChannel, users);
            LOGGER.debug("slack public channel found : " + channel.getId());
            channels.put(channel.getId(), channel);
        }

        if (jsonResponse.get("groups") != null)
        {
            JsonArray groupsJson = jsonResponse.get("groups").getAsJsonArray();
            for (JsonElement jsonObject : groupsJson)
            {
                JsonObject jsonChannel = jsonObject.getAsJsonObject();
                SlackChannelImpl channel = SlackJSONParsingUtils.buildSlackChannel(jsonChannel, users);
                LOGGER.debug("slack private group found : " + channel.getId());
                channels.put(channel.getId(), channel);
            }
        }

        if (jsonResponse.get("ims") != null)
        {
            JsonArray imsJson = jsonResponse.get("ims").getAsJsonArray();

            for (JsonElement jsonObject : imsJson)
            {
                JsonObject jsonChannel = jsonObject.getAsJsonObject();
                SlackChannelImpl channel = SlackJSONParsingUtils.buildSlackImChannel(jsonChannel, users);
                LOGGER.debug("slack im channel found : " + channel.getId());
                channels.put(channel.getId(), channel);
            }
        }


        JsonObject selfJson = jsonResponse.get("self").getAsJsonObject();
        sessionPersona = SlackJSONParsingUtils.buildSlackUser(selfJson);

        JsonObject teamJson = jsonResponse.get("team").getAsJsonObject();
        team = SlackJSONParsingUtils.buildSlackTeam(teamJson);

        webSocketURL = jsonResponse.get("url").getAsString();

    }

    public SlackPersona getSessionPersona()
    {
        return sessionPersona;
    }

    public SlackTeam getTeam()
    {
        return team;
    }
}
