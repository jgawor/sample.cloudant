package example.nosql;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Part;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/favorites")
/**
 * CRUD service of todo list table. It uses REST style.
 */
public class ResourceServlet {
/*
    @Resource(name = "couchdb/connector")
    protected CouchDbInstance db;
*/
	@POST
	public Response create(@QueryParam("id") String id, @FormParam("name") String name, @FormParam("value") String value)
			throws Exception {

	    CouchDbConnector db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		String idString = id == null ? null : id.toString();
		JsonObject resultObject = create(db, idString, name, value, null, null);

		System.out.println("Create Successful.");

		return Response.ok(resultObject.toString()).build();
	}

	protected JsonObject create(CouchDbConnector db, String id, String name, String value, Part part, String fileName)
			throws IOException {

		// check if document exist
		HashMap<String, Object> obj = (id == null) ? null : db.find(HashMap.class, id);

		if (obj == null) {
			// if new document

			id = String.valueOf(System.currentTimeMillis());

			// create a new document
			System.out.println("Creating new document with id : " + id);
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("name", name);
			data.put("_id", id);
			data.put("value", value);
			data.put("creation_date", new Date().toString());
			db.create(data);

			// attach the attachment object
			obj = db.find(HashMap.class, id);
		} else {
			// if existing document
			// attach the attachment object

			// update other fields in the document
			obj = db.find(HashMap.class, id);
			obj.put("name", name);
			obj.put("value", value);
			db.update(obj);
		}

		obj = db.find(HashMap.class, id);

		JsonObject resultObject = toJsonObject(obj);

		return resultObject;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("id") String id, @QueryParam("cmd") String cmd) throws Exception {

	    CouchDbConnector db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		JsonObject resultObject = new JsonObject();
		JsonArray jsonArray = new JsonArray();

		if (id == null) {
			try {
				// get all the document present in database
			    List<String> ids = db.getAllDocIds();

				if (ids.size() == 0) {
				    ids = initializeSampleData(db);
				}

				for (String idEntry : ids) {
					HashMap<String, Object> obj = db.find(HashMap.class, idEntry);
					JsonObject jsonObject = new JsonObject();

					jsonObject.addProperty("id", obj.get("_id") + "");
					jsonObject.addProperty("name", obj.get("name") + "");
					jsonObject.addProperty("value", obj.get("value") + "");					

					jsonArray.add(jsonObject);
				}
			} catch (Exception dnfe) {
				System.out.println("Exception thrown : " + dnfe.getMessage());
			}

			resultObject.addProperty("id", "all");
			resultObject.add("body", jsonArray);

			return Response.ok(resultObject.toString()).build();
		}

		// check if document exists
		HashMap<String, Object> obj = db.find(HashMap.class, id);
		if (obj != null) {
			JsonObject jsonObject = toJsonObject(obj);
			return Response.ok(jsonObject.toString()).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@DELETE
	public Response delete(@QueryParam("id") String id) {

	    CouchDbConnector db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		// check if document exist
		HashMap<String, Object> obj = db.find(HashMap.class, id);

		if (obj == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
		    db.delete(obj);

			System.out.println("Delete Successful.");

			return Response.ok("OK").build();
		}
	}

	@PUT
	public Response update(@QueryParam("id") String id, @QueryParam("name") String name,
			@QueryParam("value") String value) {

	    CouchDbConnector db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		// check if document exist
		HashMap<String, Object> obj = db.find(HashMap.class, id);

		if (obj == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			obj.put("name", name);
			obj.put("value", value);

			db.update(obj);

			System.out.println("Update Successful.");

			return Response.ok("OK").build();
		}
	}
	
	private JsonObject toJsonObject(HashMap<String, Object> obj) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("id", obj.get("_id") + "");
		jsonObject.addProperty("name", obj.get("name") + "");
		jsonObject.addProperty("value", obj.get("value") + "");
		return jsonObject;
	}
	
	/*
	 * Create a document and Initialize with sample data/attachments
	 */
	private List<String> initializeSampleData(CouchDbConnector db) throws Exception {

		String id = String.valueOf(System.currentTimeMillis());
		String name = "Sample entry";
		String value = "Sample description";

		// create a new document
		System.out.println("Creating new document with id : " + id);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("_id", id);
		data.put("name", name);		
		data.put("value", value);
		data.put("creation_date", new Date().toString());
		db.create(data);

		return db.getAllDocIds();
	}

	private CouchDbConnector getDB() throws NamingException {
	    InitialContext ctx = new InitialContext();
	    CouchDbInstance db = (CouchDbInstance) ctx.lookup("couchdb/connector");
	    System.out.println("lookup: " + db);
		return db.createConnector("my_database", true);
	}

}
