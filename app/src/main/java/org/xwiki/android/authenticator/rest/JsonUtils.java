package org.xwiki.android.authenticator.rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwiki.android.authenticator.bean.SearchResult;
import org.xwiki.android.authenticator.bean.XWikiUser;

import java.util.ArrayList;
import java.util.List;

/**
 * JsonUtils.
 */
public class JsonUtils {

    public static List<SearchResult> getSearchResults(byte[] bytes) throws JSONException {
        List<SearchResult> results = new ArrayList<>();
        String jsonString = new String(bytes);
        JSONObject objects = new JSONObject(jsonString);
        JSONArray array = objects.getJSONArray("searchResults");
        for(int i=0; i<array.length(); i++){
            SearchResult resultItem = new SearchResult();
            JSONObject jsonObject = (JSONObject) array.get(i);
            resultItem.type = jsonObject.getString("type");
            resultItem.id = jsonObject.getString("id");
            resultItem.pageFullName = jsonObject.getString("pageFullName");
            resultItem.wiki = jsonObject.getString("wiki");
            resultItem.space = jsonObject.getString("space");
            resultItem.pageName = jsonObject.getString("pageName");
            resultItem.modified = jsonObject.getString("modified");
            resultItem.author = jsonObject.getString("author");
            resultItem.version = jsonObject.getString("version");
            resultItem.score = jsonObject.getString("score");
            results.add(resultItem);
        }
        return results;
    }

    public static XWikiUser getXWikiUser(byte[] bytes) throws JSONException {
        XWikiUser user = new XWikiUser();
        String jsonString = new String(bytes);
        JSONObject object = new JSONObject(jsonString);
        user.id = object.getString("pageId");
        user.wiki = object.getString("wiki");
        user.space = object.getString("space");
        user.pageName = object.getString("pageName");
        JSONArray array = object.getJSONArray("properties");
        for(int i=0; i<array.length(); i++){
            JSONObject jsonObject = (JSONObject) array.get(i);
            String name = jsonObject.getString("name");
            if(name.equals("first_name")){
                user.firstName = jsonObject.getString("value");
            }else if(name.equals("last_name")){
                user.lastName = jsonObject.getString("value");
            }else if(name.equals("email")){
                user.email = jsonObject.getString("value");
            }else if(name.equals("phone")){
                user.phone = jsonObject.getString("value");
            }else if(name.equals("avatar")){
                user.avatar = jsonObject.getString("value");
            }else if(name.equals("company")){
                user.company = jsonObject.getString("value");
            }else if(name.equals("blog")){
                user.blog = jsonObject.getString("value");
            }else if(name.equals("blogfeed")){
                user.blogFeed = jsonObject.getString("value");
            }
        }
        return user;
    }


}
