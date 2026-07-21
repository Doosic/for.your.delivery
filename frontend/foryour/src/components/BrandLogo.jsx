function BrandLogo({ inverse = false, compact = false }) {
  return (
    <span className="inline-flex items-center gap-2.5" aria-label="FUB">
      <span
        className={`relative grid h-9 w-9 shrink-0 place-items-center overflow-hidden rounded-md ${
          inverse ? 'bg-white text-slate-950' : 'bg-slate-950 text-white'
        }`}
        aria-hidden="true"
      >
        <span className="text-lg font-black leading-none">F</span>
        <span className="absolute bottom-0 right-0 h-2.5 w-2.5 bg-cyan-400" />
        <span className="absolute right-2.5 top-0 h-1.5 w-1.5 bg-emerald-400" />
      </span>
      <span className="leading-none">
        <span className={`block text-xl font-black ${inverse ? 'text-white' : 'text-slate-950'}`}>FUB</span>
        {!compact && (
          <span className={`mt-1 block text-[10px] font-semibold uppercase ${inverse ? 'text-slate-300' : 'text-slate-500'}`}>
            Buy ahead
          </span>
        )}
      </span>
    </span>
  )
}

export default BrandLogo
