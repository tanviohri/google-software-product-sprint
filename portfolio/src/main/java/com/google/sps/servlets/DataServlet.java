// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.sps.data.Comment;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery results = datastore.prepare(query);

        List<Comment> comments = new ArrayList<>();

        for (Entity entity : results.asIterable()) {
            String name= (String) entity.getProperty("name");
            String commentText = (String) entity.getProperty("commentText");
            long timestamp = (long) entity.getProperty("timestamp");
            double score = (double) entity.getProperty("score");

            Comment currcomment = new Comment(name, commentText, timestamp, score);
            comments.add(currcomment);
        }

        Gson gson = new Gson();
        response.setContentType("application/json;");
        response.getWriter().println(gson.toJson(comments));
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String newComment = request.getParameter("new-comment");
        String newName = request.getParameter("new-name");

        Document doc =
        Document.newBuilder().setContent(newComment).setType(Document.Type.PLAIN_TEXT).build();
        LanguageServiceClient languageService = LanguageServiceClient.create();
        Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
        double score = sentiment.getScore();
        languageService.close();
        
        Entity commentEntity = new Entity("Comment");
        
        commentEntity.setProperty("name", newName);
        commentEntity.setProperty("commentText", newComment);
        commentEntity.setProperty("timestamp", System.currentTimeMillis());
        commentEntity.setProperty("score", score);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(commentEntity);

        response.sendRedirect("/index.html#comments");
    }
}
