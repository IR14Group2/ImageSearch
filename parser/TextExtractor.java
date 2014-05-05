import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.helper.*;
import org.jsoup.select.*;
import org.jsoup.safety.*;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

@XmlRootElement(name = "add")
class Add {
	@XmlElement(name = "doc")
	public ArrayList<Doc> docs = new ArrayList<Doc>();
}

@XmlRootElement(name = "doc")
class Doc {
	public ArrayList<Field> fields = new ArrayList<Field>();
}

@XmlRootElement(name = "field")
class Field {
	@XmlAttribute(name = "name")
	public String name;
	
	@XmlValue
	public String content;
	
	public Field() {}
	public Field(String n, String c) { name = n; content = c; }
}

public class TextExtractor {
	
	public static String NodeToString(Node node) {
		String nodeName = node.nodeName();
		if (nodeName.equals("#text")) {
			return ((TextNode)node).text().trim();
		}
		
		return ((Element)node).text().trim();
	}
	
	public static String join(String delimeter, ArrayList<String> elements) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<elements.size(); i++) {
			if (i != 0) sb.append(delimeter);
			sb.append(elements.get(i));
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException, JAXBException {
		Whitelist whitelist = new Whitelist().addTags("h1", "h2", "h3", "h4", "br").addTags("img")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addProtocols("img", "src", "http", "https");
                
                if (args.length != 2) {
			System.err.println("Arguments: url outfile.xml");
			System.exit(1);
                }
                
                String url = args[0];
                String outFile = args[1];
		
		
		Document doc = Jsoup.connect(url).get();
		//Document doc = Jsoup.connect("http://en.wikipedia.org/wiki/Apsara").get();
		String pageTitle = doc.select("title").text().trim();
		doc = new Cleaner(whitelist).clean(doc);
		doc = Jsoup.parse(doc.toString().replace("<br />", "\n"), doc.baseUri());
		
		
		System.out.println(doc);
		
		Add add = new Add();
		
		Elements images = doc.select("img");
		String allText = doc.select("body").get(0).text();
		
		for (Element img : images) {
			System.out.println("Bild: " + img.attr("abs:src") + ", alt: " + img.attr("alt") + ", size: " + img.attr("width") + "x" + img.attr("height"));
			
			Doc d = new Doc();
			add.docs.add(d);
			
			Node node = img;
			
			String headlineBefore = null;
			int headlineBeforeDist = 1<<30;
			String headlineAfter = null;
			int headlineAfterDist = 1<<30;
			ArrayList<String> before = new ArrayList<String>();
			ArrayList<String> after = new ArrayList<String>();
			int cnt, len;
			cnt = 0;
			len = 0;
			while ((node = node.previousSibling()) != null) {
				String nodeName = node.nodeName();
				String text = NodeToString(node);
				if (text.length() == 0) continue;
				if (nodeName.charAt(0) == 'h' && headlineBefore == null) {
					headlineBefore = text;
					headlineBeforeDist = cnt;
				}
				len += text.length();
				before.add(text);
				cnt++;
				if (len > 2000) break;
			}
			cnt = 0;
			len = 0;
			node = img;
			while ((node = node.nextSibling()) != null) {
				String nodeName = node.nodeName();
				String text = NodeToString(node);
				if (text.length() == 0) continue;
				if (nodeName.charAt(0) == 'h' && headlineAfter == null) {
					headlineAfter = text;
					headlineAfterDist = cnt;
				}
				len += text.length();
				after.add(text);
				cnt++;
				if (len > 4000) break;
			}
			System.out.println("H" + headlineBefore);
			System.out.println("H" + headlineAfter);
			for(int i=0; i<before.size(); i++) {
				System.out.println((-i-1) + ": " + before.get(i));
			}
			for(int i=0; i<after.size(); i++) {
				System.out.println((i+1) + ": " + after.get(i));
			}
			System.out.println();
			
			if (headlineBeforeDist < headlineAfterDist && headlineBefore != null) {
				d.fields.add(new Field("ir_header", headlineBefore));
			} else if (headlineAfter != null) {
				d.fields.add(new Field("ir_header", headlineAfter));
			}
			
			d.fields.add(new Field("ir_picture_url", img.attr("abs:src")));
			d.fields.add(new Field("ir_site_url", url));
			d.fields.add(new Field("ir_title", pageTitle));
			d.fields.add(new Field("ir_alt", img.attr("alt")));
			d.fields.add(new Field("ir_img_title", img.attr("title")));
			d.fields.add(new Field("ir_img_width", img.attr("width")));
			d.fields.add(new Field("ir_img_height", img.attr("height")));
			d.fields.add(new Field("ir_text1", (before.size() > 0 ? before.get(0) + (after.size() > 0 ? " " : "") : "") + (after.size() > 0 ? after.get(0) : "")));
			
			ArrayList<String> nearText2 = new ArrayList<String>();
			for(int i=4; i --> 0;) {
				if (before.size() > i) nearText2.add(before.get(i));
			}
			for(int i=0; i<6 && i<after.size(); i++) {
				nearText2.add(after.get(i));
			}
			d.fields.add(new Field("ir_text2", join(" ", nearText2)));
			ArrayList<String> nearText3 = new ArrayList<String>();
			for(int i=before.size(); i --> 0;) {
				nearText3.add(before.get(i));
			}
			for(int i=0; i<after.size(); i++) {
				nearText3.add(after.get(i));
			}
			d.fields.add(new Field("ir_text3", join(" ", nearText3)));
			d.fields.add(new Field("ir_all_text", allText));
		}
		
		for (Element h1 : doc.select("h2")) {
			System.out.println(h1.text());
		}
		
		JAXBContext context = JAXBContext.newInstance(Add.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(add, new java.io.File(outFile));
		
	}
}