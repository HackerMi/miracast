
package com.milink.milink.common;

import android.util.Base64;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class IQ {

    public enum Type {
        Undefined, Event, Set, Get, Result, Error;

        public static String toString(Type type) {
            String str = null;

            switch (type) {
                case Event:
                    str = "event";
                    break;

                case Error:
                    str = "error";
                    break;

                case Get:
                    str = "get";
                    break;

                case Result:
                    str = "result";
                    break;

                case Set:
                    str = "set";
                    break;

                default:
                    str = "undefined";
                    break;
            }

            return str;
        }

        public static Type toType(String type) {
            Type t = Type.Undefined;

            if (type.equalsIgnoreCase("event")) {
                t = Type.Event;
            } else if (type.equalsIgnoreCase("set")) {
                t = Type.Set;
            } else if (type.equalsIgnoreCase("get")) {
                t = Type.Get;
            } else if (type.equalsIgnoreCase("result")) {
                t = Type.Result;
            } else if (type.equalsIgnoreCase("error")) {
                t = Type.Error;
            }

            return t;
        }
    }

    private Type mType = Type.Undefined;
    private String mId = null;
    private String mXmlns = null;
    private String mAction = null;
    private String mEvent = null;
    private byte[] mParam = null;

    public static IQ create(byte bytes[]) {
        if (bytes == null)
            return null;

        IQ iq = new IQ();
        if (!iq.load(bytes))
            return null;

        return iq;
    }

    public IQ() {
        mType = Type.Undefined;
    }

    public IQ(Type type, String id, String xmlns, String actionOrEvent, byte[] param) {
        create(type, id, xmlns, actionOrEvent, param);
    }

    public IQ(Type type, int id, String xmlns, String actionOrEvent, byte[] param) {
        String idStr = String.format("%d", id);
        create(type, idStr, xmlns, actionOrEvent, param);
    }

    public void create(Type type, String id, String xmlns, String actionOrEvent, byte[] param) {
        mType = type;
        mId = id;
        mXmlns = xmlns;

        if (type == Type.Event) {
            mEvent = actionOrEvent;
        } else {
            mAction = actionOrEvent;
        }

        mParam = param;
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        mType = type;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getXmlns() {
        return mXmlns;
    }

    public void setXmlns(String xmlns) {
        mXmlns = xmlns;
    }

    public String getAction() {
        return mAction;
    }

    public void setAction(String action) {
        mAction = action;
    }

    public String getEvent() {
        return mEvent;
    }

    public void setEvent(String event) {
        mEvent = event;
    }

    public byte[] getParam() {
        return mParam;
    }

    public void setParam(byte[] param) {
        mParam = param;
    }

    public boolean load(byte bytes[]) {
        boolean result = false;

        do {
            if (bytes == null)
                break;

            InputStream is = new ByteArrayInputStream(bytes);
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();

            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(is);

                Element root = document.getDocumentElement();
                if (root == null)
                    break;

                if (!root.getTagName().equalsIgnoreCase("iq"))
                    break;

                String type = root.getAttribute("type");
                if (type == null)
                    break;

                mType = Type.toType(type);

                mId = root.getAttribute("id");
                if (mId == null)
                    break;

                Element tagQuery = getTag(root, "query");
                if (tagQuery == null)
                    break;

                mXmlns = tagQuery.getAttribute("xmlns");
                if (mXmlns == null)
                    break;

                if (mType == Type.Event) {
                    mEvent = tagQuery.getAttribute("event");
                    if (mEvent == null)
                        break;
                } else {
                    mAction = tagQuery.getAttribute("action");
                    if (mAction == null)
                        break;
                }

                Element tagParam = getTag(tagQuery, "param");
                if (tagParam == null)
                    break;

                String param = tagParam.getTextContent();
                if (param != null) {
                    mParam = Base64.decode(tagParam.getTextContent(),
                            Base64.DEFAULT);
                } else {
                    mParam = null;
                }

                result = true;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (DOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (false);

        return result;
    }

    private Element getTag(Element node, String tag) {
        if (node == null)
            return null;

        NodeList tags = node.getElementsByTagName("*");
        for (int i = 0; i < tags.getLength(); ++i) {
            Element child = (Element) tags.item(i);
            if (child.getTagName().equalsIgnoreCase(tag)) {
                return child;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        if (mType == Type.Undefined)
            return null;

        String paramBase64 = (mParam == null) ? "" : Base64.encodeToString(
                mParam, Base64.DEFAULT);

        String iq = String
                .format("<iq type=\"%s\" id=\"%s\"><query xmlns=\"%s\" %s=\"%s\"><param>%s</param></query></iq>",
                        Type.toString(mType), mId, mXmlns,
                        (mType == Type.Event) ? "event" : "action",
                        (mType == Type.Event) ? mEvent : mAction, paramBase64);

        return iq;
    }
}
