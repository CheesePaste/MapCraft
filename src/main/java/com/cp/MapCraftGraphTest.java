package com.cp;

import com.cp.data.RecipeCollector;
import com.cp.data.RecipeGraph;
import com.cp.data.RecipeNode;
import com.cp.data.RecipeEdge;
import com.cp.util.RecipeDataExporter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MapCraftGraphTest implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("MapCraftTest");
	private RecipeCollector collector;

	public MapCraftGraphTest()
	{

	}
	public MapCraftGraphTest(RecipeCollector collector)
	{
		this.collector=collector;
	}
	@Override
	public void onInitialize() {
		LOGGER.info("MapCraft 数据测试模块初始化");

		// 注册服务器启动事件
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("服务器已启动，开始收集配方数据...");
			testRecipeCollection(server);
		});
	}

	private void testRecipeCollection(net.minecraft.server.MinecraftServer server) {
		try {
			// 1. 收集所有配方
			long startTime = System.currentTimeMillis();
			collector.collectAllRecipes(server);
			long endTime = System.currentTimeMillis();
			LOGGER.info("配方收集完成，耗时: {}ms", endTime - startTime);

			// 2. 获取关系图
			RecipeGraph graph = collector.getRecipeGraph();

			// 3. 打印基本统计信息
			printGraphStatistics(graph);

			// 4. 测试特定配方
			testSpecificItems(graph);

			// 5. 验证图的结构完整性
			validateGraphStructure(graph);

			// 6. 将完整数据导出到文件
			String exportPath = exportRecipeData(graph);
			if (exportPath != null) {
				LOGGER.info("配方数据已导出到: {}", exportPath);
			}

			// 7. 导出特定物品的详细信息
			exportSpecificItemDetails(graph);

			// 8. 清理旧的导出文件（保留最近的5个）
			RecipeDataExporter.cleanupOldExports(
					RecipeDataExporter.getDefaultExportDir(),
					"mapcraft",
					5
			);

			// 9. 打印摘要（控制台输出保持不变）
			collector.printGraphSummary();

		} catch (Exception e) {
			LOGGER.error("配方收集测试失败: ", e);
		}
	}

	private String exportRecipeData(RecipeGraph graph) {
		try {
			String exportDir = RecipeDataExporter.getDefaultExportDir();

			// 导出完整配方图
			String jsonPath = RecipeDataExporter.exportRecipeGraphToJson(
					graph,
					exportDir,
					"mapcraft"
			);

			if (jsonPath != null) {
				LOGGER.info("配方关系图已保存为JSON文件: {}", jsonPath);
			}

			return jsonPath;

		} catch (Exception e) {
			LOGGER.error("导出配方数据失败: ", e);
			return null;
		}
	}

	private void exportSpecificItemDetails(RecipeGraph graph) {
		// 导出一些重要物品的详细信息
		try {
			String exportDir = RecipeDataExporter.getDefaultExportDir();

			// 导出关键物品的详细信息
			net.minecraft.item.Item[] importantItems = {
					Items.OAK_PLANKS,
					Items.CRAFTING_TABLE,
					Items.IRON_INGOT,
					Items.DIAMOND,
					Items.NETHERITE_INGOT
			};

			for (net.minecraft.item.Item item : importantItems) {
				String itemPath = RecipeDataExporter.exportItemDetails(
						graph,
						item,
						exportDir,
						"mapcraft"
				);

				if (itemPath != null) {
					LOGGER.debug("物品 {} 的详细信息已导出: {}",
							Registries.ITEM.getId(item),
							itemPath);
				}
			}

		} catch (Exception e) {
			LOGGER.error("导出特定物品详情失败: ", e);
		}
	}

	private void printGraphStatistics(RecipeGraph graph) {
		LOGGER.info("=== 关系图统计信息 ===");
		LOGGER.info("总节点数: {}", graph.getNodeCount());
		LOGGER.info("总边数: {}", graph.getEdgeCount());
		LOGGER.info("构建时间: {}", new java.util.Date(graph.getBuildTimestamp()));

		// 检查是否有循环依赖
		LOGGER.info("是否存在循环依赖: {}", graph.hasCycles());

		// 统计不同类型的关系
		Map<String, Integer> edgeTypeCount = new java.util.HashMap<>();
		for (RecipeEdge edge : graph.getEdges()) {
			String type = edge.getRelationshipType();
			edgeTypeCount.put(type, edgeTypeCount.getOrDefault(type, 0) + 1);
		}

		LOGGER.info("关系类型分布:");
		for (Map.Entry<String, Integer> entry : edgeTypeCount.entrySet()) {
			LOGGER.info("  {}: {}", entry.getKey(), entry.getValue());
		}
	}

	private void testSpecificItems(RecipeGraph graph) {
		LOGGER.info("\n=== 特定物品测试 ===");

		// 测试1: 木板的配方
		testItem(graph, Items.OAK_PLANKS, "橡木木板");

		// 测试2: 工作台的配方
		testItem(graph, Items.CRAFTING_TABLE, "工作台");

		// 测试3: 铁锭的配方
		testItem(graph, Items.IRON_INGOT, "铁锭");

		// 测试4: 铁剑的配方
		testItem(graph, Items.IRON_SWORD, "铁剑");

		// 测试5: 钻石镐的配方
		testItem(graph, Items.DIAMOND_PICKAXE, "钻石镐");
	}

	private void testItem(RecipeGraph graph, net.minecraft.item.Item item, String itemName) {
		LOGGER.info("\n--- 测试物品: {} ---", itemName);

		// 获取生产这个物品的所有配方
		List<RecipeNode> producers = graph.getNodesByOutputItem(item);
		LOGGER.info("  生产配方数量: {}", producers.size());

		for (RecipeNode producer : producers) {
			LOGGER.info("    配方ID: {}", producer.getRecipeId());
			LOGGER.info("    输出数量: {}", producer.getOutputCount());
			LOGGER.info("    输入物品数: {}", producer.getInputItems().size());

			// 获取这个节点的边
			List<RecipeEdge> outgoingEdges = graph.getEdgesFromNode(producer.getRecipeId());
			List<RecipeEdge> incomingEdges = graph.getEdgesToNode(producer.getRecipeId());
			LOGGER.info("    出边数: {}, 入边数: {}", outgoingEdges.size(), incomingEdges.size());

			// 打印前3个出边
			int count = Math.min(3, outgoingEdges.size());
			for (int i = 0; i < count; i++) {
				RecipeEdge edge = outgoingEdges.get(i);
				RecipeNode target = graph.getNodeById(edge.getToRecipeId());
				if (target != null) {
					LOGGER.info("      出边{}: -> {} (类型: {})",
							i + 1,
							target.getRecipeId(),
							edge.getRelationshipType());
				}
			}
		}

		// 获取使用这个物品作为输入的所有配方
		List<RecipeNode> consumers = graph.getNodesByInputItem(item);
		LOGGER.info("  消费配方数量: {}", consumers.size());

		// 示例：打印前3个消费者
		int consumerCount = Math.min(3, consumers.size());
		for (int i = 0; i < consumerCount; i++) {
			RecipeNode consumer = consumers.get(i);
			LOGGER.info("    消费者{}: {}", i + 1, consumer.getRecipeId());
		}
	}

	private void validateGraphStructure(RecipeGraph graph) {
		LOGGER.info("\n=== 图结构验证 ===");

		int missingNodeCount = 0;
		int selfLoopCount = 0;
		int duplicateEdgeCount = 0;

		// 检查边连接的有效性
		java.util.Set<String> edgeSet = new java.util.HashSet<>();

		for (RecipeEdge edge : graph.getEdges()) {
			// 检查节点是否存在
			if (graph.getNodeById(edge.getFromRecipeId()) == null) {
				LOGGER.warn("边引用了不存在的源节点: {}", edge.getFromRecipeId());
				missingNodeCount++;
			}
			if (graph.getNodeById(edge.getToRecipeId()) == null) {
				LOGGER.warn("边引用了不存在的目标节点: {}", edge.getToRecipeId());
				missingNodeCount++;
			}

			// 检查自环
			if (edge.getFromRecipeId().equals(edge.getToRecipeId())) {
				LOGGER.warn("发现自环: {}", edge.getFromRecipeId());
				selfLoopCount++;
			}

			// 检查重复边
			String edgeKey = edge.getFromRecipeId() + "->" + edge.getToRecipeId() + ":" + edge.getRelationshipType();
			if (edgeSet.contains(edgeKey)) {
				LOGGER.warn("发现重复边: {}", edge);
				duplicateEdgeCount++;
			} else {
				edgeSet.add(edgeKey);
			}
		}

		LOGGER.info("验证结果:");
		LOGGER.info("  缺失节点引用: {}", missingNodeCount);
		LOGGER.info("  自环数量: {}", selfLoopCount);
		LOGGER.info("  重复边数量: {}", duplicateEdgeCount);

		if (missingNodeCount == 0 && selfLoopCount == 0 && duplicateEdgeCount == 0) {
			LOGGER.info("  图结构验证通过 ✓");
		} else {
			LOGGER.warn("  图结构存在问题，需要修复");
		}
	}

	// 额外的测试方法：查找特定配方的关系链
	public static void findRecipeChain(RecipeGraph graph, Identifier startRecipeId) {
		LOGGER.info("\n=== 查找配方关系链: {} ===", startRecipeId);

		RecipeNode startNode = graph.getNodeById(startRecipeId);
		if (startNode == null) {
			LOGGER.warn("配方不存在: {}", startRecipeId);
			return;
		}

		// 深度优先搜索，限制深度
		java.util.Set<Identifier> visited = new java.util.HashSet<>();
		java.util.Stack<Object[]> stack = new java.util.Stack<>();
		stack.push(new Object[]{startRecipeId, 0, ""});

		int maxDepth = 3;
		int pathCount = 0;

		while (!stack.isEmpty() && pathCount < 10) {
			Object[] current = stack.pop();
			Identifier currentId = (Identifier) current[0];
			int depth = (Integer) current[1];
			String path = (String) current[2];

			if (visited.contains(currentId) || depth > maxDepth) {
				continue;
			}

			visited.add(currentId);
			RecipeNode currentNode = graph.getNodeById(currentId);

			if (currentNode != null) {
				String outputName = Registries.ITEM.getId(currentNode.getOutputItem()).toString();
				LOGGER.info("{}{} -> {} (输入: {})",
						"  ".repeat(depth),
						currentId,
						outputName,
						currentNode.getInputItems().size());

				// 添加出边到栈中
				for (RecipeEdge edge : graph.getEdgesFromNode(currentId)) {
					String newPath = path + " -> " + edge.getToRecipeId();
					stack.push(new Object[]{edge.getToRecipeId(), depth + 1, newPath});
					pathCount++;
				}
			}
		}
	}
}