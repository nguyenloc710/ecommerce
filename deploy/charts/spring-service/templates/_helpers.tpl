{{- define "spring-service.name" -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "spring-service.labels" -}}
app.kubernetes.io/name: {{ include "spring-service.name" . }}
app.kubernetes.io/part-of: ecommerce
app.kubernetes.io/version: {{ .Values.image.tag | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "spring-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "spring-service.name" . }}
{{- end -}}
