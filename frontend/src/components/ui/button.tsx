import { cva, type VariantProps } from 'class-variance-authority'
import type { ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/utils'

const buttonVariants = cva('inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-indigo-200 disabled:pointer-events-none disabled:opacity-50', {
  variants: {
    variant: {
      default: 'bg-slate-950 text-white shadow-lg shadow-slate-950/15 hover:-translate-y-0.5 hover:bg-slate-800',
      secondary: 'border border-slate-200 bg-white text-slate-700 shadow-sm hover:-translate-y-0.5 hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700',
      ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
      accent: 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/20 hover:-translate-y-0.5 hover:bg-indigo-500',
    },
    size: { default: 'w-full', sm: 'min-h-10 px-3 py-2 text-xs', lg: 'min-h-12 px-6 text-base' },
  },
  defaultVariants: { variant: 'default', size: 'default' },
})

export function Button({ className, variant, size, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & VariantProps<typeof buttonVariants>) {
  return <button className={cn(buttonVariants({ variant, size, className }))} {...props} />
}
