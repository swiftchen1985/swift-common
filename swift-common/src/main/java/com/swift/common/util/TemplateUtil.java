package com.swift.common.util;

import java.io.File;
import java.io.FileWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import freemarker.template.Configuration;

/**
 * 模板工具类
 * Created by swift on 2017/3/3.
 */
public class TemplateUtil {

	/**
	 * VelocityTemplet 根据模板生成文件
	 * @param inputVmFilePath 模板路径
	 * @param outputFilePath 输出文件路径
	 * @param context
	 * @throws Exception
	 */
	
	public static class VelocityTemplate {
		public static void generate(String inputVmFilePath, String outputFilePath, VelocityContext context) throws Exception {
			try {
				Velocity.init();
				VelocityEngine engine = new VelocityEngine();
				Template template = engine.getTemplate(inputVmFilePath, "utf-8");
				File outputFile = new File(outputFilePath);
				FileWriter writer = new FileWriter(outputFile);
				template.merge(context, writer);
				writer.close();
			} catch (Exception ex) {
				throw ex;
			}
		}
	}
	
	/**
	 * FreeMarkTemplate 根据模板生成文件
	 * @param inputVmFilePath 模板路径
	 * @param outputFilePath 输出文件路径
	 * @param context
	 * @throws Exception
	 */
	public static class FreeMarkTemplate {
		public static void generate(String inputVmFilePath, String outputFilePath, VelocityContext context) throws Exception {
			try {
				 Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
			} catch (Exception ex) {
				throw ex;
			}
		}
	}

}
