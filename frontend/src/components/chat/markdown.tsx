"use client";

/**
 * Randarea unică a textului venit de la model și de la utilizator.
 *
 * DE CE `skipHtml` este obligatoriu: răspunsul modelului este input necontrolat. Un tag HTML
 * aparent inofensiv poate ajunge dintr-un document injectat în prompt și nu trebuie executat în UI.
 * DE CE limbajele sunt înregistrate individual: chatul nu are nevoie de întreaga librărie Prism,
 * iar bundle-ul trebuie să plătească doar pentru limbajele pe care le afișează.
 */

import { useState } from "react";
import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { PrismLight as SyntaxHighlighter } from "react-syntax-highlighter";
import bash from "react-syntax-highlighter/dist/esm/languages/prism/bash";
import javascript from "react-syntax-highlighter/dist/esm/languages/prism/javascript";
import json from "react-syntax-highlighter/dist/esm/languages/prism/json";
import python from "react-syntax-highlighter/dist/esm/languages/prism/python";
import sql from "react-syntax-highlighter/dist/esm/languages/prism/sql";
import tsx from "react-syntax-highlighter/dist/esm/languages/prism/tsx";
import typescript from "react-syntax-highlighter/dist/esm/languages/prism/typescript";
import { oneDark } from "react-syntax-highlighter/dist/esm/styles/prism";
import { Copy } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

SyntaxHighlighter.registerLanguage("ts", typescript);
SyntaxHighlighter.registerLanguage("typescript", typescript);
SyntaxHighlighter.registerLanguage("tsx", tsx);
SyntaxHighlighter.registerLanguage("js", javascript);
SyntaxHighlighter.registerLanguage("javascript", javascript);
SyntaxHighlighter.registerLanguage("json", json);
SyntaxHighlighter.registerLanguage("bash", bash);
SyntaxHighlighter.registerLanguage("sh", bash);
SyntaxHighlighter.registerLanguage("sql", sql);
SyntaxHighlighter.registerLanguage("python", python);
SyntaxHighlighter.registerLanguage("py", python);

const supportedLanguages = new Set([
  "ts",
  "typescript",
  "tsx",
  "js",
  "javascript",
  "json",
  "bash",
  "sh",
  "sql",
  "python",
  "py"
]);

type MarkdownProps = {
  children: string;
  className?: string;
};

function CodeBlock({ children, className }: MarkdownProps) {
  const [copied, setCopied] = useState(false);
  const code = children.replace(/\n$/, "");
  const language = className?.replace("language-", "") ?? "text";
  const registeredLanguage = supportedLanguages.has(language) ? language : "text";

  async function handleCopy() {
    if (!navigator.clipboard) {
      toast.error("Copierea este disponibilă doar pe localhost sau HTTPS.");
      return;
    }

    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      toast.success("Cod copiat");
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      toast.error("Browserul nu a permis copierea în clipboard.");
    }
  }

  return (
    <div className="group relative my-3 overflow-hidden rounded-lg border bg-[#282c34]">
      <Tooltip>
        <TooltipTrigger
          render={
            <Button
              variant="ghost"
              size="icon-xs"
              onClick={() => void handleCopy()}
              aria-label={copied ? "Cod copiat" : "Copiază codul"}
              className="absolute top-2 right-2 z-10 text-white/70 opacity-0 group-hover:opacity-100 hover:bg-white/10 hover:text-white focus-visible:opacity-100"
            />
          }
        >
          <Copy />
        </TooltipTrigger>
        <TooltipContent>{copied ? "Copiat" : "Copiază codul"}</TooltipContent>
      </Tooltip>
      <SyntaxHighlighter
        language={registeredLanguage}
        style={oneDark}
        customStyle={{ margin: 0, padding: "1rem", paddingRight: "3rem", background: "transparent" }}
        codeTagProps={{ className: "text-sm" }}
      >
        {code}
      </SyntaxHighlighter>
    </div>
  );
}

const components: Components = {
  a: ({ className, ...props }) => (
    <a {...props} target="_blank" rel="noopener noreferrer" className={cn("underline underline-offset-4", className)} />
  ),
  code: ({ className, children, ...props }) => {
    const value = String(children);
    const isBlock = Boolean(className?.startsWith("language-")) || value.includes("\n");

    if (!isBlock) {
      return (
        <code {...props} className={cn("rounded bg-background/70 px-1 py-0.5 font-mono text-[0.9em]", className)}>
          {children}
        </code>
      );
    }

    return <CodeBlock className={className}>{value}</CodeBlock>;
  },
  h1: ({ className, ...props }) => <h1 {...props} className={cn("mt-4 mb-2 text-xl font-semibold", className)} />,
  h2: ({ className, ...props }) => <h2 {...props} className={cn("mt-4 mb-2 text-lg font-semibold", className)} />,
  h3: ({ className, ...props }) => <h3 {...props} className={cn("mt-3 mb-1 font-semibold", className)} />,
  ul: ({ className, ...props }) => <ul {...props} className={cn("my-2 list-disc space-y-1 pl-5", className)} />,
  ol: ({ className, ...props }) => <ol {...props} className={cn("my-2 list-decimal space-y-1 pl-5", className)} />,
  table: ({ className, ...props }) => (
    <div className="my-3 overflow-x-auto rounded-lg border">
      <table {...props} className={cn("w-full text-left text-sm", className)} />
    </div>
  ),
  th: ({ className, ...props }) => <th {...props} className={cn("border-b px-3 py-2 font-semibold", className)} />,
  td: ({ className, ...props }) => <td {...props} className={cn("border-b px-3 py-2 align-top", className)} />,
  pre: ({ children }) => <>{children}</>
};

export function Markdown({ children, className }: MarkdownProps) {
  return (
    <div className={cn("min-w-0 space-y-2 break-words", className)}>
      <ReactMarkdown components={components} remarkPlugins={[remarkGfm]} skipHtml>
        {children}
      </ReactMarkdown>
    </div>
  );
}
