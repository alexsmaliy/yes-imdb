package com.alexsmaliy.yesimdb.service.scraper;

import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("scraper")
@Produces(MediaType.APPLICATION_JSON)
public interface ScraperResource {
    @POST
    @Path("query")
    @Timed
    List<String> query(String query);

    @POST
    @Path("admin")
    @Timed
    String admin(ScraperCommand request);
}
