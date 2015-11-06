
package com.test.miracast;

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

public class ParamStart {

    private String mIp = null;
    private int mPort = 0;

    public static ParamStart create(byte bytes[]) {
        if (bytes == null)
            return null;

        ParamStart param = new ParamStart();
        if (!param.load(bytes))
            return null;

        return param;
    }

    public static ParamStart create(String ip, int port) {
        ParamStart param = new ParamStart();
        param.mIp = ip;
        param.mPort = port;

        return param;
    }

    public String getIp() {
        return mIp;
    }

    public void setIp(String mIp) {
        this.mIp = mIp;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int mPort) {
        this.mPort = mPort;
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

                if (!root.getTagName().equalsIgnoreCase("root"))
                    break;

                Element tagIp = getTag(root, "ip");
                if (tagIp == null)
                    break;

                Element tagPort = getTag(root, "port");
                if (tagPort == null)
                    break;

                mIp = tagIp.getTextContent();

                try {
                    mPort = Integer.valueOf(tagPort.getTextContent());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    mPort = 0;
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
        String str = String.format("<root><ip>%s</ip><port>%d</port></root>", mIp, mPort);
        return str;
    }
}
