package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"
)

// LLMProvider abstracts language model inference.
type LLMProvider interface {
	GenerateReply(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error)
}

// OpenAICompatibleProvider calls any OpenAI-compatible completions endpoint.
type OpenAICompatibleProvider struct {
	client  *http.Client
	baseURL string
	model   string
	apiKey  string
}

// NewOpenAICompatibleProvider creates a new provider instance.
func NewOpenAICompatibleProvider(baseURL, model, apiKey string) *OpenAICompatibleProvider {
	if baseURL == "" {
		baseURL = "https://api.openai.com/v1/chat/completions"
	}
	if model == "" {
		model = "gpt-4o-mini"
	}
	return &OpenAICompatibleProvider{
		client:  &http.Client{Timeout: 20 * time.Second},
		baseURL: baseURL,
		model:   model,
		apiKey:  apiKey,
	}
}

type openAIChatMessage struct {
	Role    string `json:"role"`
	Content string `json:"content"`
}

type openAIChatRequest struct {
	Model       string              `json:"model"`
	Messages    []openAIChatMessage `json:"messages"`
	Temperature float32             `json:"temperature"`
}

type openAIChatResponse struct {
	Choices []struct {
		Message struct {
			Role    string `json:"role"`
			Content string `json:"content"`
		} `json:"message"`
	} `json:"choices"`
	Error *struct {
		Message string `json:"message"`
	} `json:"error,omitempty"`
}

func (p *OpenAICompatibleProvider) GenerateReply(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error) {
	if p.apiKey == "" {
		return "", errors.New("llm api key is not configured")
	}

	payloadMessages := make([]openAIChatMessage, 0, len(messages)+1)
	if systemPrompt != "" {
		payloadMessages = append(payloadMessages, openAIChatMessage{
			Role:    "system",
			Content: systemPrompt,
		})
	}
	for _, m := range messages {
		payloadMessages = append(payloadMessages, openAIChatMessage{
			Role:    m.Role,
			Content: m.Content,
		})
	}

	reqBody, err := json.Marshal(openAIChatRequest{
		Model:       p.model,
		Messages:    payloadMessages,
		Temperature: 0.2,
	})
	if err != nil {
		return "", fmt.Errorf("failed to encode llm request: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.baseURL, bytes.NewReader(reqBody))
	if err != nil {
		return "", fmt.Errorf("failed to create llm http request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+p.apiKey)

	resp, err := p.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("upstream llm request failed: %w", err)
	}
	defer resp.Body.Close()

	bodyBytes, err := io.ReadAll(io.LimitReader(resp.Body, 1024*1024))
	if err != nil {
		return "", fmt.Errorf("failed to read llm response body: %w", err)
	}

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("upstream llm returned status code %d", resp.StatusCode)
	}

	var parsed openAIChatResponse
	if err := json.Unmarshal(bodyBytes, &parsed); err != nil {
		return "", fmt.Errorf("failed to decode llm response json: %w", err)
	}

	if len(parsed.Choices) == 0 || strings.TrimSpace(parsed.Choices[0].Message.Content) == "" {
		return "", errors.New("upstream llm returned empty completion")
	}

	return parsed.Choices[0].Message.Content, nil
}

// AnthropicProvider calls the Anthropic Messages API.
type AnthropicProvider struct {
	client  *http.Client
	baseURL string
	model   string
	apiKey  string
}

func NewAnthropicProvider(baseURL, model, apiKey string) *AnthropicProvider {
	if baseURL == "" {
		baseURL = "https://api.anthropic.com/v1/messages"
	}
	if model == "" {
		model = "claude-3-5-haiku-20241022"
	}
	return &AnthropicProvider{
		client:  &http.Client{Timeout: 20 * time.Second},
		baseURL: baseURL,
		model:   model,
		apiKey:  apiKey,
	}
}

type anthropicRequest struct {
	Model     string              `json:"model"`
	MaxTokens int                 `json:"max_tokens"`
	System    string              `json:"system,omitempty"`
	Messages  []openAIChatMessage `json:"messages"`
}

type anthropicResponse struct {
	Content []struct {
		Type string `json:"type"`
		Text string `json:"text"`
	} `json:"content"`
}

func (p *AnthropicProvider) GenerateReply(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error) {
	if p.apiKey == "" {
		return "", errors.New("llm api key is not configured")
	}

	payloadMessages := make([]openAIChatMessage, 0, len(messages))
	for _, m := range messages {
		payloadMessages = append(payloadMessages, openAIChatMessage{
			Role:    m.Role,
			Content: m.Content,
		})
	}

	reqBody, err := json.Marshal(anthropicRequest{
		Model:     p.model,
		MaxTokens: 1024,
		System:    systemPrompt,
		Messages:  payloadMessages,
	})
	if err != nil {
		return "", fmt.Errorf("failed to encode anthropic request: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.baseURL, bytes.NewReader(reqBody))
	if err != nil {
		return "", fmt.Errorf("failed to create anthropic request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("x-api-key", p.apiKey)
	req.Header.Set("anthropic-version", "2023-06-01")

	resp, err := p.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("upstream anthropic request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("upstream anthropic returned status code %d", resp.StatusCode)
	}

	var parsed anthropicResponse
	if err := json.NewDecoder(resp.Body).Decode(&parsed); err != nil {
		return "", fmt.Errorf("failed to decode anthropic response: %w", err)
	}

	for _, block := range parsed.Content {
		if block.Type == "text" && strings.TrimSpace(block.Text) != "" {
			return block.Text, nil
		}
	}

	return "", errors.New("empty text response from anthropic")
}

// MockProvider is used for unit tests or offline simulations.
type MockProvider struct {
	ReplyFunc func(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error)
}

func (m *MockProvider) GenerateReply(ctx context.Context, systemPrompt string, messages []ChatMessage) (string, error) {
	if m.ReplyFunc != nil {
		return m.ReplyFunc(ctx, systemPrompt, messages)
	}
	return "1. Call 112 immediately if life-threatening.\n2. Keep the patient calm.\nCRITICAL: false", nil
}

// NewLLMProviderFromEnv constructs the configured LLM provider based on environment variables.
// Config is read strictly from environment variables: LLM_PROVIDER=openai, LLM_MODEL, LLM_API_KEY.
// Fails if LLM_API_KEY is missing (unless LLM_PROVIDER=mock). Never logs the API key.
func NewLLMProviderFromEnv() (LLMProvider, error) {
	provider := strings.ToLower(strings.TrimSpace(os.Getenv("LLM_PROVIDER")))
	if provider == "" {
		provider = "openai"
	}
	model := strings.TrimSpace(os.Getenv("LLM_MODEL"))
	if model == "" {
		model = "gpt-4o-mini"
	}
	apiKey := strings.TrimSpace(os.Getenv("LLM_API_KEY"))
	if apiKey == "" && provider == "openai" {
		apiKey = strings.TrimSpace(os.Getenv("OPENAI_API_KEY"))
	}
	baseURL := strings.TrimSpace(os.Getenv("LLM_BASE_URL"))

	if provider == "mock" {
		return &MockProvider{}, nil
	}

	if apiKey == "" {
		return nil, errors.New("LLM_API_KEY environment variable is required but not set")
	}

	switch provider {
	case "anthropic":
		return NewAnthropicProvider(baseURL, model, apiKey), nil
	case "gemini":
		if baseURL == "" {
			baseURL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
		}
		return NewOpenAICompatibleProvider(baseURL, model, apiKey), nil
	case "openai":
		return NewOpenAICompatibleProvider(baseURL, model, apiKey), nil
	default:
		return nil, fmt.Errorf("unsupported LLM_PROVIDER %q (supported: openai, mock, anthropic, gemini)", provider)
	}
}
