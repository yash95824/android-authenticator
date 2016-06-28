package org.xwiki.android.authenticator.rest;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.android.authenticator.bean.SearchResult;
import org.xwiki.android.authenticator.bean.XWikiUser;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by lf on 2016/6/28.
 */
public class JsonUtilsText {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void jsonSearchResultTest() throws Exception{
        //String url = "http://210.76.195.123:8080/xwiki/rest/wikis/query?q=wiki:xwiki%20and%20object:XWiki.XWikiUsers%20and%20date:[2016-06-19T02:49:25Z%20TO%20*]&number=10000&media=json";
        String query = ", BaseObject as userObj , XWikiDocument as groupDoc , BaseObject as groupObj , com.xpn.xwiki.objects.StringProperty as groupObj_member1 where ( (groupObj_member1.value = doc.fullName or groupObj_member1.value = CONCAT('xwiki:', doc.fullName)) and groupDoc.fullName = 'XWiki.XWikiTest' ) and doc.fullName=userObj.name and userObj.className='XWiki.XWikiUsers' and groupDoc.fullName=groupObj.name and groupObj.className='XWiki.XWikiGroups' and groupObj_member1.id.id=groupObj.id and groupObj_member1.id.name='member'";
        query = URLEncoder.encode(query, "UTF-8");
        String url = "http://210.76.195.123:8080/xwiki/rest/wikis/xwiki/query?q=" + query +
                "&type=hql&media=json";
        HttpRequest request = new HttpRequest(url);
        HttpExecutor httpExecutor = new HttpExecutor();
        HttpResponse response = httpExecutor.performRequest(request);
        byte[] contentData = response.getContentData();
        List<SearchResult> results = JsonUtils.getSearchResults(contentData);
        System.out.println(results);
    }

    @Test
    public void jsonXWikiUserTest() throws Exception{
        String url = "http://210.76.195.123:8080/xwiki/rest/wikis/xwiki/spaces/XWiki/pages/bbb/objects/XWiki.XWikiUsers/0?&media=json";
        HttpRequest request = new HttpRequest(url);
        HttpExecutor httpExecutor = new HttpExecutor();
        HttpResponse response = httpExecutor.performRequest(request);
        byte[] contentData = response.getContentData();
        XWikiUser user = JsonUtils.getXWikiUser(contentData);
        System.out.println(user);
        Assert.assertNotNull(user);
    }

}
