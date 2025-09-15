package converters

import kotlinx.datetime.Clock
import models.*

fun OpenCodeModel.toOpenAIModel(provider: OpenCodeProvider): Model = Model(
    id = this.id,
    created = Clock.System.now().epochSeconds,
    ownedBy = this.id.substringBefore("/"),
    name = this.name,
    canonicalSlug = this.id.substringBefore(":"),
    description = generateDescription(this, provider),
    contextLength = this.limit.context,
    architecture = Architecture(
        modality = "${this.modalities.input.joinToString("+")}->${this.modalities.output.joinToString("+")}",
        inputModalities = this.modalities.input,
        outputModalities = this.modalities.output
    ),
    pricing = Pricing(
        prompt = this.cost.input.toString(),
        completion = this.cost.output.toString()
    ),
    topProvider = TopProvider(
        contextLength = this.limit.context,
        maxCompletionTokens = this.limit.output.takeIf { it > 0 }
    ),
    supportedParameters = generateSupportedParameters(this)
)

private fun generateSupportedParameters(model: OpenCodeModel): List<String> = buildList {
    add("max_tokens")
    if (model.temperature) add("temperature")
    if (model.toolCall) addAll(listOf("tool_choice", "tools"))
    if (model.reasoning) addAll(listOf("reasoning", "include_reasoning"))
    addAll(listOf("stop", "seed"))
}

private fun generateDescription(model: OpenCodeModel, provider: OpenCodeProvider): String {
    val capabilities = listOfNotNull(
        "reasoning".takeIf { model.reasoning },
        "tool calling".takeIf { model.toolCall },
        "file attachments".takeIf { model.attachment }
    )

    val modality = when {
        "image" in model.modalities.input -> if ("text" in model.modalities.input) "multimodal" else "vision"
        else -> "text"
    }

    val contextK = model.limit.context / 1000
    val isFree = model.cost.input == 0.0 && model.cost.output == 0.0

    return buildString {
        append("${model.name} from ${provider.name}")
        if (capabilities.isNotEmpty()) append(" with ${capabilities.joinToString(", ")}")
        append(". ${modality.replaceFirstChar { it.uppercase() }} model with ${contextK}K context")
        if (isFree) append(" (free)")
        append(".")
    }
}