"use client";

/**
 * Ghidul „Cum folosesc aplicația”: un dialog cu file, generat din registrul `lib/guide.ts`.
 *
 * DE CE dialog și nu o pagină: ca și Preferințele, ghidul e o paranteză din care te întorci exact unde erai (conversația
 * rămâne neatinsă). Starea de deschidere stă în store, deci îl deschid sidebar-ul, tasta `?` și dialogul de bun venit.
 * DE CE rendererele sunt un registru pe tipul blocului: un tip nou de bloc e o intrare aici, nu un `if` în JSX.
 */
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { GuideBlockDeleteData } from "@/components/guide/guide-block-delete-data";
import { GuideBlockGlossary } from "@/components/guide/guide-block-glossary";
import { GuideBlockPrompts } from "@/components/guide/guide-block-prompts";
import { GuideBlockText } from "@/components/guide/guide-block-text";
import { useGuideShortcut } from "@/hooks/use-guide-shortcut";
import { GUIDE_SECTIONS, type GuideBlock } from "@/lib/guide";
import { useAppStore } from "@/store/useAppStore";

const BLOCK_RENDERERS: Record<GuideBlock["kind"], (props: { block: GuideBlock }) => React.ReactNode> = {
  text: GuideBlockText,
  prompts: GuideBlockPrompts,
  glossary: GuideBlockGlossary,
  "delete-data": GuideBlockDeleteData
};

export function GuideDialog() {
  const open = useAppStore(state => state.guideOpen);
  const setOpen = useAppStore(state => state.setGuideOpen);
  useGuideShortcut();

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="flex h-[min(40rem,90svh)] w-full flex-col gap-3 overflow-hidden sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>Cum folosesc aplicația</DialogTitle>
          <DialogDescription>
            Ce poate face consilierul, cum obții răspunsuri bune și cum citești rezultatele. Deschizi ghidul oricând cu
            tasta ?.
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue={GUIDE_SECTIONS[0].id} className="min-h-0 flex-1">
          {/* Pe ecran îngust filele se derulează pe orizontală; `h-auto` lasă etichetele să-și ia înălțimea firească. */}
          <TabsList className="h-auto w-full justify-start overflow-x-auto">
            {GUIDE_SECTIONS.map(section => (
              <TabsTrigger key={section.id} value={section.id} className="flex-none px-2.5 py-1.5">
                <section.icon />
                {section.label}
              </TabsTrigger>
            ))}
          </TabsList>

          {GUIDE_SECTIONS.map(section => (
            <TabsContent key={section.id} value={section.id} className="min-h-0 overflow-y-auto pr-1">
              <div className="flex flex-col gap-5 py-2">
                {section.blocks.map(block => {
                  const Renderer = BLOCK_RENDERERS[block.kind];
                  return <Renderer key={block.id} block={block} />;
                })}
              </div>
            </TabsContent>
          ))}
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
